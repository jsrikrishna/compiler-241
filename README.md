# compiler-241
Compiler for Language 241

 - Parses the source code according to CFG Grammar defined at 
 - Compiler implements the follwing techniques
	 - Common SubExpression Elimination using Static Single Assignment
	 - Register Allocation
	 - Live Range Analysis to minimize the number of Register Allocated
	 - Remove DeadCode while performing Live Range Analysis
	 - Copy Propagation
	 - Removal of PHI Instructions introduced during SSA

