# compiler-241
Compiler for Language 241

 - Parses the source code according to CFG Grammar defined at 
 - Compiler implements the follwing techniques
	 - Common SubExpression Elimination and Copy Propagation using Static Single Assignment
	 - Control Flow Graphs are generated to visualize Common SubExpression Elimination and Copy Propagation
	 - Register Allocation
	 - Live Range Analysis (LRA) to used to track the variables in the program while allocating Register. Live Ranges are visulaized using Interference Graphs.
	 - Remove DeadCode while performing Live Range Analysis
	 - Removal of PHI Instructions introduced by SSA

