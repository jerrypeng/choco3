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
package parser.flatzinc.ast.constraints.global;

import parser.flatzinc.ast.Datas;
import parser.flatzinc.ast.constraints.IBuilder;
import parser.flatzinc.ast.expression.EAnnotation;
import parser.flatzinc.ast.expression.EInt;
import parser.flatzinc.ast.expression.Expression;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.IntConstraintFactory;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.VariableFactory;

import java.util.List;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 26/07/12
 */
public class CountEqReifBuilder implements IBuilder {
    @Override
    public Constraint[] build(Solver solver, String name, List<Expression> exps, List<EAnnotation> annotations, Datas datas) {
		IntVar[] x = exps.get(0).toIntVarArray(solver);
        IntVar c = exps.get(2).intVarValue(solver);
        if (exps.get(1) instanceof EInt) {
            int y = exps.get(1).intValue();
            return new Constraint[]{IntConstraintFactory.count(y, x, c)};
        }
        IntVar y = exps.get(1).intVarValue(solver);
		BoolVar b = exps.get(3).boolVarValue(solver);
        if (y.isInstantiated()) {
			Constraint cstr = IntConstraintFactory.count(y.getValue(), x, c);
			cstr.reifyWith(b);
            return new Constraint[]{};
        } else {
            int ylb = y.getLB();
            int yub = y.getUB();
            int nb = yub - ylb + 1;
			IntVar[] cs = VariableFactory.boundedArray("cs", nb, 0, nb, solver);
            Constraint[] cstrs = new Constraint[yub - ylb + 1];
            int k = 0;
            for (int i = ylb; i <= yub; i++) {
                cstrs[k++] = IntConstraintFactory.count(i, x, cs[i - ylb]);
            }
			Constraint cstr = IntConstraintFactory.element(c, cs, y, ylb);
			cstr.reifyWith(b);
            return cstrs;
        }
    }
}