#!/usr/bin/env python3
"""Local exploratory forecasts from CSV or OpenVitals PWA backups."""
import argparse
import csv
from datetime import date, timedelta
import json
import math
from pathlib import Path

UNITS = {"weight": "kg", "restingHeart": "bpm", "hrv": "ms", "steps": "steps",
         "sleep": "hours", "bodyFat": "%", "leanMass": "kg", "bodyWater": "kg"}


def read_points(path: Path, metric: str):
    if path.suffix.lower() == ".json":
        payload = json.loads(path.read_text())
        if not isinstance(payload, dict) or not isinstance(payload.get("records"), list):
            raise ValueError("Expected an OpenVitals PWA backup with a records list")
        rows = payload["records"]
        column = metric
    else:
        with path.open(newline="") as stream:
            reader = csv.DictReader(stream)
            column = metric if metric in (reader.fieldnames or []) else "value"
            rows = list(reader)
    points = []
    for row in rows:
        value = row.get(column)
        if value is None or value == "":
            continue
        day = date.fromisoformat(str(row.get("date", "")))
        if isinstance(value, bool):
            raise ValueError("Boolean measurements are invalid")
        number = float(value)
        if not math.isfinite(number):
            raise ValueError("Measurements must be finite")
        points.append((day, number))
    points.sort()
    if len(points) < 14:
        raise ValueError("At least 14 daily observations are required")
    for previous, current in zip(points, points[1:]):
        if (current[0] - previous[0]).days != 1:
            raise ValueError("Dates must be unique and daily; missing days are not filled or interpolated")
    return points


def forecast(points, metric, horizon):
    if not 1 <= horizon <= 30:
        raise ValueError("Horizon must be between 1 and 30 days")
    import darts
    import pandas as pd
    from darts import TimeSeries
    from darts.metrics import mae
    from darts.models.forecasting.baselines import NaiveDrift, NaiveSeasonal

    series = TimeSeries.from_times_and_values(
        pd.DatetimeIndex([day for day, _ in points]),
        [value for _, value in points], freq="D")
    holdout = min(7, max(3, len(points) // 4))
    train, actual = series[:-holdout], series[-holdout:]
    drift = NaiveDrift()
    drift.fit(train)
    drift_error = float(mae(actual, drift.predict(holdout)))
    baseline = NaiveSeasonal(K=1)
    baseline.fit(train)
    baseline_error = float(mae(actual, baseline.predict(holdout)))
    drift.fit(series)
    predicted = drift.predict(horizon).univariate_values()
    return {
        "metric": metric, "unit": UNITS[metric], "observations": len(points),
        "last_observation": points[-1][0].isoformat(), "darts_version": darts.__version__,
        "model": "NaiveDrift", "evaluation": {
            "holdout_days": holdout, "drift_mae": drift_error,
            "last_value_mae": baseline_error, "drift_beats_last_value": drift_error < baseline_error,
        },
        "forecast": [{"date": (points[-1][0] + timedelta(days=i + 1)).isoformat(),
                      "value": round(float(value), 6)} for i, value in enumerate(predicted)],
        "scope": "Exploratory linear extrapolation; no confidence interval or clinical interpretation",
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="CSV (date,value or date,metric) or PWA JSON backup")
    parser.add_argument("--metric", choices=list(UNITS), default="weight")
    parser.add_argument("--horizon", type=int, default=7)
    args = parser.parse_args()
    try:
        result = forecast(read_points(args.input, args.metric), args.metric, args.horizon)
    except (ValueError, KeyError, TypeError, OSError) as error:
        parser.error(str(error))
    print(json.dumps(result, indent=2, allow_nan=False))


if __name__ == "__main__":
    main()
