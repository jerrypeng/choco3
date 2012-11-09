/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package solver.recorders.fine.arc;

import solver.ICause;
import solver.Solver;
import solver.constraints.propagators.Propagator;
import solver.propagation.IPropagationEngine;
import solver.recorders.conditions.ICondition;
import solver.variables.EventType;
import solver.variables.Variable;

/**
 * A specialized fine event recorder associated with one variable and one propagator.
 * It observes a variable, records events occurring on the variable,
 * schedules it self when calling the filtering algortithm of the propagator
 * is required.
 * It also stores, if required, pointers to value removals.
 * <br/>
 * on a pris le parti de ne pas m�moriser les �v�nements fins,
 * partant du principe que ca sera de toute facon plus couteux � d�piler et � traiter
 * que de lancer directement la propag' lourde
 *
 * @author Charles Prud'homme
 * @since 01/12/11
 */
public class ArcEventRecorderWithCondition<V extends Variable> extends ArcEventRecorder<V> {

    protected int idxVinP; // index of the variable within the propagator -- immutable

    final ICondition condition; // condition to run the filtering algorithm of the propagator

    public ArcEventRecorderWithCondition(V variable, Propagator<V> propagator, int idxInProp,
                                         ICondition condition, Solver solver, IPropagationEngine engine) {
        super(variable, propagator, solver, engine);
        this.idxVinP = idxInProp;
        this.condition = condition;
        condition.linkRecorder(this);
    }

    @Override
    public void afterUpdate(int vIdx, EventType evt, ICause cause) {
        // Only notify constraints that filter on the specific event received
        assert cause != null : "should be Cause.Null instead";
        if (cause != propagators[PINDEX]) { // due to idempotency of propagator, it should not be schedule itself
            if (propagators[PINDEX].advise(idxVinP, evt.mask)) {
                // schedule this if condition is valid
                if (condition.validateScheduling(this, propagators[PINDEX], evt)) {
                    propagators[PINDEX].forcePropagate(EventType.FULL_PROPAGATION);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "<< " + variables[VINDEX].toString() + "::" + propagators[PINDEX].toString() + "::" + condition.toString() + " >>";
    }
}
