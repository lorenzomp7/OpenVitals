#!/usr/bin/env python3
"""OpenVitals MCP adapter for the pinned upstream Vibetest runner."""
import os

# Set these before importing browser-use, which configures its logging on import.
os.environ["ANONYMIZED_TELEMETRY"] = "false"
os.environ["BROWSER_USE_LOGGING_LEVEL"] = "CRITICAL"
import sys
from contextlib import redirect_stdout

# MCP stdout must contain JSON-RPC only, including during dependency imports.
with redirect_stdout(sys.stderr):
    import vibetest.agents as upstream
    from browser_use.llm import ChatGoogle
    from mcp.server.mcpserver import MCPServer
    from mcp.server.mcpserver.exceptions import ToolError


def configured_google(**kwargs):
    key = os.environ.get("GOOGLE_API_KEY")
    model = os.environ.get("VIBETEST_MODEL")
    if not key or not model:
        raise ValueError("Set GOOGLE_API_KEY and VIBETEST_MODEL before running AI QA")
    kwargs["api_key"] = key
    kwargs["model"] = model
    return ChatGoogle(**kwargs)


# All three upstream call sites (scout, QA, summary) use this factory.
# No edits to site-packages and no hardcoded retired model identifiers.
upstream.ChatGoogle = configured_google


class CompatibleBrowserSession(upstream.BrowserSession):
    async def aclose(self):
        # Upstream Vibetest calls the older aclose API during every cleanup.
        await self.stop()


upstream.BrowserSession = CompatibleBrowserSession
server = MCPServer("openvitals-vibetest")


@server.tool()
async def start(url: str = "http://127.0.0.1:8765", num_agents: int = 3, headless: bool = True) -> str:
    """Test the OpenVitals PWA with synthetic records. Uses the configured Google model.

    Start scripts/ai/serve-pwa.sh first. QA should cover mobile layout, period
    navigation, metric details, synthetic profile data and backup validation.
    This does not test Android or Health Connect. Model calls may incur costs.
    """
    from urllib.parse import urlparse
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise ToolError("An absolute HTTP(S) URL is required")
    if not 1 <= num_agents <= 10:
        raise ToolError("num_agents must be between 1 and 10")
    try:
        configured_google()  # Fail before browser launch when configuration is missing.
    except ValueError as error:
        raise ToolError(str(error)) from error
    with redirect_stdout(sys.stderr):
        return await upstream.run_pool(url, num_agents, headless=headless)


@server.tool()
async def results(test_id: str) -> dict:
    """Read the consolidated Vibetest report in this server process."""
    with redirect_stdout(sys.stderr):
        report = await upstream.summarize_bug_reports(test_id)
    if "llm_analysis" in report:
        report["llm_analysis"]["model_used"] = os.environ.get("VIBETEST_MODEL")
    run = upstream._test_results.get(test_id)
    if run:
        expected = run["agents"]
        succeeded = report.get("successful_agents", 0)
        report["expected_agents"] = expected
        if succeeded < expected:
            report.update(overall_status="incomplete", status_emoji="⚠️",
                          status_description="QA did not complete for all requested agents; inspect errors")
    return report


if __name__ == "__main__":
    server.run()
