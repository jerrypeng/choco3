/**
 * Copyright (c) 2014,
 *       Charles Prud'homme (TASC, INRIA Rennes, LINA CNRS UMR 6241),
 *       Jean-Guillaume Fages (COSLING S.A.S.).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chocosolver.solver.constraints.extension.binary;

import gnu.trove.map.hash.THashMap;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;

/**
 * AC2001 algorithm for binary table constraint
 * <br/>
 *
 * @author Charles Prud'homme, Hadrien Cambazard
 * @since 08/06/11
 */
public class PropBinAC2001 extends PropBinCSP {

    protected IStateInt[] currentSupport0;
    protected IStateInt[] currentSupport1;

    protected int offset0;
    protected int offset1;

    public PropBinAC2001(IntVar x, IntVar y, Tuples tuples) {
        this(x, y, new CouplesTable(tuples, x, y));
    }

    private PropBinAC2001(IntVar x, IntVar y, CouplesTable table) {
        super(x, y, table);
        offset0 = x.getLB();
        offset1 = y.getLB();
        currentSupport0 = new IStateInt[x.getUB() - offset0 + 1];
        currentSupport1 = new IStateInt[y.getUB() - offset1 + 1];
        IEnvironment environment = solver.getEnvironment();
        for (int i = 0; i < currentSupport0.length; i++) {
            currentSupport0[i] = environment.makeInt();
            currentSupport0[i].set(-1);
        }
        for (int i = 0; i < currentSupport1.length; i++) {
            currentSupport1[i] = environment.makeInt();
            currentSupport1[i].set(-1);
        }

    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int support = 0;
        boolean found = false;
        int left = Integer.MIN_VALUE;
        int right = left;
        int ub0 = vars[0].getUB();
        for (int val0 = vars[0].getLB(); val0 <= ub0; val0 = vars[0].nextValue(val0)) {
            int ub1 = vars[1].getUB();
            for (int val1 = vars[1].getLB(); val1 <= ub1; val1 = vars[1].nextValue(val1)) {
                if (relation.isConsistent(val0, val1)) {
                    support = val1;
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (val0 == right + 1) {
                    right = val0;
                } else {
                    vars[0].removeInterval(left, right, aCause);
                    left = val0;
                    right = val0;
                }
            } else
                currentSupport0[val0 - offset0].set(support);

            found = false;
        }
        vars[0].removeInterval(left, right, aCause);

        found = false;
        right = left = Integer.MIN_VALUE;
        int ub1 = vars[1].getUB();
        for (int val1 = vars[1].getLB(); val1 <= ub1; val1 = vars[1].nextValue(val1)) {
            ub0 = vars[0].getUB();
            for (int val0 = vars[0].getLB(); val0 <= ub0; val0 = vars[0].nextValue(val0)) {
                if (relation.isConsistent(val0, val1)) {
                    support = val0;
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (val1 == right + 1) {
                    right = val1;
                } else {
                    vars[1].removeInterval(left, right, aCause);
                    left = val1;
                    right = val1;
                }
            } else
                currentSupport1[val1 - offset1].set(support);
            found = false;
        }
        vars[1].removeInterval(left, right, aCause);
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        if (IntEventType.isInstantiate(mask)) {
            onInstantiationOf(idxVarInProp);
        } else {
            if (idxVarInProp == 0) {
                reviseV1();
            } else {
                reviseV0();
            }
        }
    }

    @Override
    public String toString() {
        return "Bin_AC2001(" + vars[0].getName() + ", " + vars[1].getName() + ", " + this.relation.getClass().getSimpleName() + ")";
    }

    @Override
    public void duplicate(Solver solver, THashMap<Object, Object> identitymap) {
        if (!identitymap.containsKey(this)) {
            this.vars[0].duplicate(solver, identitymap);
            IntVar X = (IntVar) identitymap.get(this.vars[0]);
            this.vars[1].duplicate(solver, identitymap);
            IntVar Y = (IntVar) identitymap.get(this.vars[1]);

            identitymap.put(this, new PropBinAC2001(X, Y, (CouplesTable) relation.duplicate()));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * updates the support for all values in the domain of v1, and remove unsupported values for v1
     *
     * @throws ContradictionException
     */
    private void reviseV1() throws ContradictionException {
        int left = Integer.MIN_VALUE;
        int right = left;
        int ub1 = vars[1].getUB();
        for (int val1 = vars[1].getLB(); val1 <= ub1; val1 = vars[1].nextValue(val1)) {
            if (!vars[0].contains(currentSupport1[val1 - offset1].get())) {
                boolean found = false;
                int support = currentSupport1[val1 - offset1].get();
                int max1 = vars[0].getUB();
                while (!found && support < max1) {
                    support = vars[0].nextValue(support);
                    if (relation.isConsistent(support, val1)) found = true;
                }
                if (found) {
                    currentSupport1[val1 - offset1].set(support);
                } else {
                    if (val1 == right + 1) {
                        right = val1;
                    } else {
                        vars[1].removeInterval(left, right, aCause);
                        left = right = val1;
                    }
                }
            }
        }
        vars[1].removeInterval(left, right, aCause);
    }

    /**
     * updates the support for all values in the domain of v0, and remove unsupported values for v0
     *
     * @throws ContradictionException
     */
    private void reviseV0() throws ContradictionException {
        int left = Integer.MIN_VALUE;
        int right = left;
        int ub0 = vars[0].getUB();
        for (int val0 = vars[0].getLB(); val0 <= ub0; val0 = vars[0].nextValue(val0)) {
            if (!vars[1].contains(currentSupport0[val0 - offset0].get())) {
                boolean found = false;
                int support = currentSupport0[val0 - offset0].get();
                int max2 = vars[1].getUB();
                while (!found && support < max2) {
                    support = vars[1].nextValue(support);
                    if (relation.isConsistent(val0, support)) found = true;
                }
                if (found)
                    currentSupport0[val0 - offset0].set(support);
                else {
                    if (val0 == right + 1) {
                        right = val0;
                    } else {
                        vars[0].removeInterval(left, right, aCause);
                        left = right = val0;
                    }
                }
            }
        }
        vars[0].removeInterval(left, right, aCause);
    }

    private void onInstantiationOf(int idx) throws ContradictionException {
        if (idx == 0) {
            int value = vars[0].getValue();
            int left = Integer.MIN_VALUE;
            int right = left;
            int ub1 = vars[1].getUB();
            for (int val1 = vars[1].getLB(); val1 <= ub1; val1 = vars[1].nextValue(val1)) {
                if (!relation.isConsistent(value, val1)) {
                    if (val1 == right + 1) {
                        right = val1;
                    } else {
                        vars[1].removeInterval(left, right, aCause);
                        left = val1;
                        right = val1;
                    }
                }
            }
            vars[1].removeInterval(left, right, aCause);
        } else {
            int value = vars[1].getValue();
            int left = Integer.MIN_VALUE;
            int right = left;
            int ub0 = vars[0].getUB();
            for (int val0 = vars[0].getLB(); val0 <= ub0; val0 = vars[0].nextValue(val0)) {
                if (!relation.isConsistent(val0, value)) {
                    if (val0 == right + 1) {
                        right = val0;
                    } else {
                        vars[0].removeInterval(left, right, aCause);
                        left = val0;
                        right = val0;
                    }
                }
            }
            vars[0].removeInterval(left, right, aCause);
        }
    }
}
