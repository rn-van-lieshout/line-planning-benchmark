# Best Solutions

This folder contains 972 solution files — one per problem instance — each representing the best solution found across all experiments.

## Naming convention

```
{Network}_{ODMatrix}_B{Budget}_TP{TransferPenalty}_LG{LineGeneration}_LA{LineActivationCost}.txt
```

| Segment | Parameter | Values |
|---|---|---|
| `{Network}` | Network | `Randstad-IC`, `Randstad-IC-SPR`, `Netherlands-IC`, `Netherlands-IC-SPR`, `Switzerland-Small`, `Switzerland-Large` |
| `{ODMatrix}` | OD matrix variant | `dense`, `sparse` |
| `B{Budget}` | Line activation budget | `small`, `medium`, `large` |
| `TP{TransferPenalty}` | Transfer penalty | `small`, `medium`, `large` |
| `LG{LineGeneration}` | Line generation setting | `small`, `medium`, `large` |
| `LA{LineActivationCost}` | Line activation cost | `low`, `medium`, `high` |

**Example:** `Randstad-IC_sparse_Bmedium_TPlarge_LGsmall_LAlow.txt`

The same `dense`/`sparse` labels are used for the `ODMatrix` field inside each file.

## File structure

Each solution file has four sections separated by blank lines.

### `# Settings`

Records the instance configuration used to generate the solution.

| Field | Description |
|---|---|
| `Instance` | Path to the instance folder (e.g. `instances/Randstad-IC`) |
| `ODMatrix` | OD matrix variant: `dense` or `sparse` |
| `TotalVolume` | Sum of all OD demand volumes |
| `TransferPenalty` | Transfer penalty level and value in minutes (e.g. `small (5)`) |
| `LineGeneration` | Line pool size setting: `small`, `medium`, or `large` |
| `Budget` | Budget level and value in cost units (e.g. `large (718)`) |
| `LineActivationCost` | Fixed cost per activated line: `low`, `medium`, or `high` |
| `Solver` | Solver variant used (e.g. `DPTNSolver-BAP`, `CGNSolver-cuts`, `PTNSolver`) |
| `RouteGeneration` | *(route-based solvers only)* Route set size: `small`, `medium`, or `large` |
| `StrongInequalities` | *(branch-and-price solvers only)* Whether BP&C cuts were enabled |
| `TimeLimitSeconds` | CPU time limit given to the solver |

### `# Results`

Reports solver output and re-evaluated metrics.

| Field | Description |
|---|---|
| `Feasible` | Whether a feasible solution was found |
| `ObjectiveEstimate` | Solver's objective value (uses the model's internal passenger routing approximation) |
| `Cost` | Total line operation cost; must be ≤ budget |
| `ComputationTimeSeconds` | Actual wall-clock time used |
| `OptimalityGap` | Relative gap between the best integer solution and the lower bound |
| `DirectTravelersEstimatePct` | % of demand traveling without transfer (model estimate) |
| `OneTransferEstimatePct` | % of demand with exactly one transfer (model estimate) |
| `TwoPlusTransfersEstimatePct` | % of demand with two or more transfers (model estimate) |
| `ObjectiveActual` | True objective: re-evaluated by fixing the line plan and solving passenger routing exactly over the CGN |
| `UncoveredDemandActualPct` | % of demand that cannot reach its destination given the line plan |
| `DirectTravelersActualPct` | % of demand traveling without transfer (CGN re-evaluation) |
| `OneTransferActualPct` | % of demand with exactly one transfer (CGN re-evaluation) |
| `TwoPlusTransfersActualPct` | % of demand with two or more transfers (CGN re-evaluation) |

All solutions in this folder have been re-evaluated; `ObjectiveActual` is the value stored in `bestSolutions/overview.csv` and used for comparison.

### `# LinePlan`

Machine-readable representation of the line plan. Each line corresponds to one operated train line:

```
{frequency} {stationId} {stationId} ...
```

`frequency` is the number of trains per hour. The remaining integers are station IDs (from `stations.txt`) in the order the line visits them.

### `# Human-Readable Line Plan`

One entry per operated line, in the format:

```
f={frequency}   {type}   {stationName} - {stationName} - ... - {stationName}  ({duration} min)
```

`type` is `IC` (InterCity) or `SPR` (Sprinter). `duration` is the end-to-end travel time in minutes.
