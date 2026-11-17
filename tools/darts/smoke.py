#!/usr/bin/env python3
"""Check real Darts forecasts and OpenVitals adapters using synthetic inputs."""
import csv
from datetime import date, timedelta
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from forecast import forecast, read_points


def main():
    with tempfile.TemporaryDirectory(prefix="openvitals-darts-") as directory:
        root = Path(directory)
        records = [{"date": (date(2026, 1, 1) + timedelta(days=i)).isoformat(),
                    "weight": 70 + i * 0.1} for i in range(28)]
        backup = root / "synthetic.json"
        backup.write_text(json.dumps({"records": records}))
        points = read_points(backup, "weight")
        report = forecast(points, "weight", 3)
        assert len(report["forecast"]) == 3
        assert abs(report["forecast"][0]["value"] - 72.8) < 1e-6
        assert report["forecast"][0]["date"] == "2026-01-29"
        assert report["evaluation"]["drift_mae"] < 1e-8
        assert report["evaluation"]["drift_beats_last_value"]
        csv_path = root / "synthetic.csv"
        with csv_path.open("w", newline="") as stream:
            writer = csv.writer(stream)
            writer.writerow(["date", "value"])
            writer.writerows((record["date"], record["weight"]) for record in records)
        assert read_points(csv_path, "weight") == points
        output = subprocess.check_output([sys.executable, str(Path(__file__).with_name("forecast.py")), str(backup), "--horizon", "3"], text=True)
        assert json.loads(output)["forecast"] == report["forecast"]
        for invalid in (records[:5], records[:5] + records[6:], records + [records[-1]],
                        records[:-1] + [{"date": records[-1]["date"], "weight": float("nan")}],
                        records[:-1] + [{"date": records[-1]["date"], "weight": True}]):
            backup.write_text(json.dumps({"records": invalid}))
            try:
                read_points(backup, "weight")
                raise AssertionError("Invalid time series accepted")
            except ValueError:
                pass
        for horizon in (0, 31):
            try:
                forecast(points, "weight", horizon)
                raise AssertionError("Invalid horizon accepted")
            except ValueError:
                pass
    print("DARTS_OK: forecast values/dates, holdout baseline, CSV/PWA adapters, CLI JSON, invalid inputs")


if __name__ == "__main__":
    main()
