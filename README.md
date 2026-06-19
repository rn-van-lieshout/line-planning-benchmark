# Line Planning Benchmark

A benchmark library of instances and algorithms for the **Line Planning Problem**, accompanying the paper:

> [PAPER CITATION — fill in title, authors, venue, and arXiv/DOI link]

## Instance Overview

The library contains **6 instances** based on real-world Dutch and Swiss railway networks:

| Instance          | Network                          | Variant                            |
|-------------------|----------------------------------|------------------------------------|
| Randstad-IC       | Netherlands — Randstad region    | InterCity only                     |
| Randstad-IC-SPR   | Netherlands — Randstad region    | InterCity + Special Purpose Routes |
| Nederland-IC      | Netherlands — full network       | InterCity only                     |
| Nederland-IC-SPR  | Netherlands — full network       | InterCity + Special Purpose Routes |
| SmallSwitzerland  | Switzerland — partial network    | —                                  |
| Switzerland       | Switzerland — full network       | —                                  |

Each instance supports multiple problem variants:

| Dimension        | Levels              | Description                                     |
|-----------------|---------------------|-------------------------------------------------|
| Route generation | small / medium / large | Maximum passenger detour: 0 / 15 / 30 minutes |
| Line generation  | small / medium / large | Line pool size ratio: 1.1 / 1.3 / 1.5          |
| OD matrix        | Dense / Sparse      | Dense: raw demand; Sparse: demand aggregated via strategy 3 |
| Transfer penalty | small / medium / large | Cost per passenger transfer                    |
| Budget           | small / medium / large | Total budget for operating lines               |
| Fixed cost       | low / medium / high | Fixed cost per line activated                  |

## Problem Description

The Line Planning Problem asks for a minimum-cost set of *lines* — routes operated at a given frequency — such that every origin-destination passenger demand can be routed through the network. A feasible line plan must satisfy:

- A hard budget constraint on total operating cost
- Minimum frequency requirements for each selected line
- Passenger routings that respect transfer penalties and detour limits

Costs consist of a fixed activation cost per line and a variable operating cost proportional to frequency and route length.

## Repository Structure

```
instances/
    {instance-name}/
        stations.txt
        tracks.txt
        connections.txt
        forbiddenTurns.txt
        allowedTypeChanges.txt
        LineGeneration/     ← small.txt, medium.txt, large.txt
        RouteGeneration/    ← small.txt, medium.txt, large.txt
        ODMatrix/           ← 0.txt, 1-false.txt, 1-true.txt, 2-false.txt, 2-true.txt, 3-false.txt, 3-true.txt
        transferPenalty/    ← small.txt, medium.txt, large.txt
        budget/             ← small.txt, medium.txt, large.txt  (medium fixed cost)
        budget-fixedCost-low/
        budget-fixedCost-high/
        fixedCost/          ← low.txt, medium.txt, high.txt
        solutions/          ← solver output files
src/
    linePlanning/
        scripts/            ← Main.java entry point, VerifySolution.java
        solvers/            ← PTNSolver, PTNBendersSolver, DPTNSolver, CGNSolver
        instance/           ← LinePlanningInstance, LinePlanningSolution, LinePlanningParameters
        columnGeneration/   ← master problem, pricing routine, cuts
        lineGeneration/     ← line pool generator
        routeGeneration/    ← passenger route generator
        CGN/                ← Congestion Graph Network structures
        DPTN/               ← Dynamic Passenger Transfer Network structures
        PTN/                ← Passenger Transport Network structures
    graph/                  ← Dijkstra, DFS, SPPRC
    optimisation/           ← Branch-and-price framework, cutting planes
    util/                   ← Reader, Writer, Logger, Configuration
pom.xml
settings.properties
```

## Building and Running

**Requirements:** Java 21, IBM CPLEX 22.1.0 (must be installed and available on the classpath)

Build with Maven:

```bash
mvn package
```

The solver is configured by editing the index variables at the top of [src/linePlanning/scripts/Main.java](src/linePlanning/scripts/Main.java):

| Variable              | Range | Description                                                                                  |
|-----------------------|-------|----------------------------------------------------------------------------------------------|
| `instanceIndex`       | 0–5   | Randstad-IC, Randstad-IC-SPR, Nederland-IC, Nederland-IC-SPR, SmallSwitzerland, Switzerland |
| `routeGenerationIndex`| 0–2   | small (0 min detour) / medium (15 min) / large (30 min); ignored by CGNSolver               |
| `lineGenerationIndex` | 0–2   | small (ratio 1.1) / medium (1.3) / large (1.5)                                              |
| `odMatrixIndex`       | 0–1   | 0 = Dense (raw demand); 1 = Sparse (strategy 3, aggregated)                                 |
| `transferPenaltyIndex`| 0–2   | small / medium / large                                                                       |
| `budgetIndex`         | 0–2   | small / medium / large                                                                       |
| `fixedCostIndex`      | 0–2   | low / medium / high                                                                          |
| `solverIndex`         | 0–5   | see Algorithms below                                                                         |

Run after building:

```bash
java -cp target/line-planning-benchmark-0.0.1-SNAPSHOT.jar linePlanning.scripts.Main
```

Additional solver settings (time limit, strong inequalities, etc.) can be adjusted in [settings.properties](settings.properties).

## Algorithms

Four solver families are implemented, all built on a column generation framework with an optional Branch-and-Price tree:

| Solver index | Class               | Description                                                      |
|-------------|---------------------|------------------------------------------------------------------|
| 0           | `PTNSolver`         | Column generation on the Passenger Transport Network (LP relaxation only) |
| 1           | `PTNSolver`         | PTN solver with Branch-and-Price                                 |
| 2           | `PTNBendersSolver`  | PTN model solved via Benders decomposition                       |
| 3           | `DPTNSolver`        | Dynamic PTN solver (LP relaxation only)                          |
| 4           | `DPTNSolver`        | Dynamic PTN solver with Branch-and-Price                         |
| 5           | `CGNSolver`         | Congestion Graph Network solver                                  |

The pricing subproblem is a Shortest Path Problem with Resource Constraints (SPPRC), solved via labelling on the relevant graph structure. Strong inequalities (cuts) can be enabled via `useStrongInequalities` in `Main.java`.

## Citation

If you use these instances or algorithms in your research, please cite:

```
@article{[CITATION KEY TBD],
  author = {[AUTHORS]},
  title  = {[TITLE]},
  year   = {2026},
}
```

## License

This repository is released under the [MIT License](LICENSE).
