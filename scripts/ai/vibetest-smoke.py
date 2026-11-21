#!/usr/bin/env python3
"""Verify the actual MCP protocol and adapter guards without model calls."""
import asyncio
import importlib.util
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]


def adapter_checks():
    spec = importlib.util.spec_from_file_location("openvitals_vibetest", ROOT / "scripts/ai/vibetest-server.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    for key in ("GOOGLE_API_KEY", "VIBETEST_MODEL"):
        os.environ.pop(key, None)
    try:
        module.configured_google(model="retired-model")
        raise AssertionError("Missing credentials were accepted")
    except ValueError:
        pass
    os.environ["GOOGLE_API_KEY"] = "synthetic-test-key"
    os.environ["VIBETEST_MODEL"] = "synthetic-test-model"
    client = module.configured_google(model="retired-model")
    assert client.model == "synthetic-test-model"
    for key in ("GOOGLE_API_KEY", "VIBETEST_MODEL"):
        os.environ.pop(key, None)
    assert hasattr(module.upstream.BrowserSession, "aclose")
    return module


async def main():
    module = adapter_checks()
    # Validation must fail before any network or browser work.
    for url, count in (("file:///tmp/example", 1), ("http://127.0.0.1:8765", 0)):
        try:
            await module.start(url, count)
            raise AssertionError("Invalid QA invocation was accepted")
        except module.ToolError:
            pass
    module.upstream._test_results["failed-run"] = {
        "agents": 1, "results": [{"status": "error", "error": "synthetic browser failure"}]
    }
    failed = await module.results("failed-run")
    assert failed["overall_status"] == "incomplete", failed
    from mcp import ClientSession, StdioServerParameters
    from mcp.client.stdio import stdio_client
    params = StdioServerParameters(command=sys.executable, args=[str(ROOT / "scripts/ai/vibetest-server.py")], env=dict(os.environ))
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            assert {tool.name for tool in tools.tools} == {"start", "results"}
            report = await session.call_tool("results", {"test_id": "unknown-smoke-test"})
            assert not report.is_error, report
            denied = await session.call_tool("start", {"url": "http://127.0.0.1:8765", "num_agents": 1})
            assert denied.is_error, "AI QA started without configured credentials"
    print("VIBETEST_MCP_OK: imports, model override, input guards, initialize, tools/list, tools/call; no model requests")


if __name__ == "__main__":
    asyncio.run(main())
