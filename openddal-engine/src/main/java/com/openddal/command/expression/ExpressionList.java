/*
 * Copyright 2014-2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.openddal.command.expression;

import com.openddal.dbobject.table.Column;
import com.openddal.dbobject.table.ColumnResolver;
import com.openddal.dbobject.table.TableFilter;
import com.openddal.engine.Session;
import com.openddal.util.StatementBuilder;
import com.openddal.value.Value;
import com.openddal.value.ValueArray;

import java.util.List;

/**
 * A list of expressions, as in (ID, NAME).
 * The result of this expression is an array.
 */
public class ExpressionList extends Expression {

    private final Expression[] list;

    public ExpressionList(Expression[] list) {
        this.list = list;
    }

    @Override
    public Value getValue(Session session) {
        Value[] v = new Value[list.length];
        for (int i = 0; i < list.length; i++) {
            v[i] = list[i].getValue(session);
        }
        return ValueArray.get(v);
    }

    @Override
    public int getType() {
        return Value.ARRAY;
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level) {
        for (Expression e : list) {
            e.mapColumns(resolver, level);
        }
    }

    @Override
    public Expression optimize(Session session) {
        boolean allConst = true;
        for (int i = 0; i < list.length; i++) {
            Expression e = list[i].optimize(session);
            if (!e.isConstant()) {
                allConst = false;
            }
            list[i] = e;
        }
        if (allConst) {
            return ValueExpression.get(getValue(session));
        }
        return this;
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        for (Expression e : list) {
            e.setEvaluatable(tableFilter, b);
        }
    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public long getPrecision() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getDisplaySize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getSQL() {
        StatementBuilder buff = new StatementBuilder("(");
        for (Expression e : list) {
            buff.appendExceptFirst(", ");
            buff.append(e.getSQL());
        }
        if (list.length == 1) {
            buff.append(',');
        }
        return buff.append(')').toString();
    }

    @Override
    public void updateAggregate(Session session) {
        for (Expression e : list) {
            e.updateAggregate(session);
        }
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        for (Expression e : list) {
            if (!e.isEverything(visitor)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getCost() {
        int cost = 1;
        for (Expression e : list) {
            cost += e.getCost();
        }
        return cost;
    }

    @Override
    public Expression[] getExpressionColumns(Session session) {
        ExpressionColumn[] expr = new ExpressionColumn[list.length];
        for (int i = 0; i < list.length; i++) {
            Expression e = list[i];
            Column col = new Column("C" + (i + 1),
                    e.getType(), e.getPrecision(), e.getScale(),
                    e.getDisplaySize());
            expr[i] = new ExpressionColumn(session.getDatabase(), col);
        }
        return expr;
    }

    @Override
    public String exportParameters(TableFilter filter, List<Value> container) {
        StatementBuilder buff = new StatementBuilder("(");
        for (Expression e : list) {
            buff.appendExceptFirst(", ");
            buff.append(e.exportParameters(filter, container));
        }
        if (list.length == 1) {
            buff.append(',');
        }
        return buff.append(')').toString();
    }

}
