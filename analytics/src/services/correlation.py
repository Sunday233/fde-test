from __future__ import annotations

import math


def pearson(x: list[float], y: list[float]) -> float:
    """Compute Pearson correlation coefficient between two sequences."""
    n = len(x)
    if n != len(y) or n < 2:
        return 0.0
    mean_x = sum(x) / n
    mean_y = sum(y) / n
    cov = sum((xi - mean_x) * (yi - mean_y) for xi, yi in zip(x, y))
    std_x = math.sqrt(sum((xi - mean_x) ** 2 for xi in x))
    std_y = math.sqrt(sum((yi - mean_y) ** 2 for yi in y))
    if std_x == 0.0 or std_y == 0.0:
        return 0.0
    return cov / (std_x * std_y)


def correlation_matrix(data: dict[str, list[float]]) -> tuple[list[str], list[list[float]]]:
    """Compute pairwise Pearson correlation matrix.

    Args:
        data: mapping of factor name → list of values (all same length)

    Returns:
        (factor_names, matrix) where matrix[i][j] = pearson(factors[i], factors[j])
    """
    factors = list(data.keys())
    n = len(factors)
    matrix = [[0.0] * n for _ in range(n)]
    for i in range(n):
        matrix[i][i] = 1.0
        for j in range(i + 1, n):
            r = pearson(data[factors[i]], data[factors[j]])
            matrix[i][j] = round(r, 4)
            matrix[j][i] = round(r, 4)
    return factors, matrix
