/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package solver.constraints.propagators.nary.globalcardinality;

import choco.kernel.ESat;
import choco.kernel.common.util.procedure.IntProcedure1;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.constraints.propagators.nary.matching.FlowStructure;
import solver.exception.ContradictionException;
import solver.requests.IRequest;
import solver.variables.EventType;
import solver.variables.IntVar;
import solver.variables.domain.delta.IntDelta;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 15/06/11
 */
public class PropGlobalCardinalityAC extends Propagator<IntVar> {

    public FlowStructure struct;
    protected final RemProc rem_proc;
    protected final int minValue, maxValue;
    protected final int[] minFlow, maxFlow;

    /**
     * Constructor, Global cardinality constraint API
     * note : maxVal - minVal + 1 = valueMinOccurence.length = valueMaxOccurence.length
     *
     * @param vars     the variable list
     * @param minValue smallest value that could be assigned to variable
     * @param maxValue greatest value that could be assigned to variable
     * @param low      minimum for each value
     * @param up       maximum occurences for each value
     */
    @SuppressWarnings({"unchecked"})
    public PropGlobalCardinalityAC(IntVar[] vars, int minValue, int maxValue, int[] low, int[] up,
                                   Constraint constraint, Solver solver) {
        super(vars, solver, constraint, PropagatorPriority.CUBIC, true);
        this.struct = new FlowStructure(vars, vars.length, getValueGap(vars), low, up, solver);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minFlow = low;
        this.maxFlow = up;
        rem_proc = new RemProc(this);
    }

    /**
     * Static method for one parameter constructor
     *
     * @param vars domain variable list
     * @return gap between min and max value
     */
    private static int getValueGap(IntVar[] vars) {
        int minValue = Integer.MAX_VALUE, maxValue = Integer.MIN_VALUE;
        for (IntVar var : vars) {
            minValue = Math.min(var.getLB(), minValue);
            maxValue = Math.max(var.getUB(), maxValue);
        }
        return maxValue - minValue + 1;
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.ALL_MASK();
    }

    @Override
    public void propagate() throws ContradictionException {
        for (int i = 0; i < maxFlow.length; i++) {
            vars[i].updateLowerBound(minValue, this);
            vars[i].updateUpperBound(maxValue, this);
        }
        // On suppose que la structure struct est deja ete initialisee par la contrainte
        // car elle est partagee entre tous les propagateurs
        struct.removeUselessEdges(this);
    }

    @Override
    public void propagateOnRequest(IRequest<IntVar> request, int varIdx, int mask) throws ContradictionException {
        IntVar var = request.getVariable();
        IntDelta delta = var.getDelta();

        if (EventType.isInstantiate(mask)) {
            struct.setMatch(varIdx, var.getValue());
        } else {
            int f = request.fromDelta();
            int l = request.toDelta();
            delta.forEach(rem_proc.set(varIdx), f, l);
        }
        if (getNbRequestEnqued() == 0) {
            struct.removeUselessEdges(this);
        }
    }

    @Override
    public ESat isEntailed() {
        if (isCompletelyInstantiated()) {
            for (IntVar v : vars) {
                if (v.instantiated()) {
                    int vv = v.getValue();
                    for (IntVar w : vars) {
                        if (w != v) {
                            if (w.instantiated()) {
                                if (vv == w.getValue()) {
                                    return ESat.FALSE;
                                }
                            } else {
                                return ESat.UNDEFINED;
                            }
                        }
                    }
                } else {
                    return ESat.UNDEFINED;
                }
            }
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }


    private static class RemProc implements IntProcedure1<Integer> {

        private final PropGlobalCardinalityAC p;
        private int idxVar;

        public RemProc(PropGlobalCardinalityAC p) {
            this.p = p;
        }

        @Override
        public IntProcedure1 set(Integer idxVar) {
            this.idxVar = idxVar;
            return this;
        }

        @Override
        public void execute(int i) throws ContradictionException {
            p.struct.nodes[idxVar].removeEdge(i);
            p.struct.deleteMatch(idxVar, i - p.struct.getMinValue());
            //p.vars[idxVar].removeValue(i + p.minValue, this.p);
        }
    }

}