package org.apache.openjpa.jdbc.kernel.exps;

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.Expression;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PredicateValue extends AbstractVal {

    private final List<Val> values = new ArrayList<>();
    private final boolean isNot;
    private final boolean isAnd;

    public PredicateValue(boolean isNot, boolean isAnd, List<Val> values) {
        this.isNot = isNot;
        this.isAnd = isAnd;
        this.values.addAll(values);
    }

    @Override
    public ExpState initialize(Select sel, ExpContext ctx, int flags) {

        ExpState result = null;
        for (Val val : values) {
            ExpState next = val.initialize(sel, ctx, flags);
            if (result == null) {
                result = next;
            } else {
                result = new BinaryOpExpState(sel.and(result.joins, next.joins), result, next);
            }

        }

        return result;

    }

    @Override
    public void select(Select sel, ExpContext ctx, ExpState state, boolean pks) {
        sel.select(newSQLBuffer(sel, ctx, state), this);
    }

    private void unwindState(ExpState state, BiConsumer<Val, ExpState> onState) {

        Iterator<Val> i = values.iterator();
        while (true) {
            Val v = i.next();
            if (!i.hasNext()) {
                // last state is not binary, so don't split
                onState.accept(v, state);
                break;
            }

            // there are more values, so we need to split
            onState.accept(v, ((BinaryOpExpState)state).state1);
            state = ((BinaryOpExpState)state).state2;

        }

    }

    @Override
    public void selectColumns(Select sel, ExpContext ctx, ExpState state, boolean pks) {

        unwindState(state, (v,s)->v.selectColumns(sel, ctx, s, true));

    }

    @Override
    public void groupBy(Select sel, ExpContext ctx, ExpState state) {
        sel.groupBy(newSQLBuffer(sel, ctx, state));
    }

    @Override
    public void orderBy(Select sel, ExpContext ctx, ExpState state, boolean asc) {
        sel.orderBy(newSQLBuffer(sel, ctx, state), asc, false, getSelectAs());
    }

    @Override
    public Object load(ExpContext ctx, ExpState state, Result res) throws SQLException {
        return Filters.convert(res.getObject(this, JavaSQLTypes.JDBC_DEFAULT,
                null), getType());
    }

    @Override
    public void calculateValue(Select sel, ExpContext ctx, ExpState state, Val other, ExpState otherState) {
        // I honestly don't understand what's supposed to happen here.
        unwindState(state, (v, s)-> v.calculateValue(sel, ctx, s, null, null));
    }

    @Override
    public int length(Select sel, ExpContext ctx, ExpState state) {
        // I also don't understand this.
        return 1;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state, SQLBuffer sql, int index) {

    }

    @Override
    public Class getType() {
        return null;
    }

    @Override
    public void setImplicitType(Class type) {

    }

    @Override
    public ClassMetaData getMetaData() {
        return null;
    }

    @Override
    public void setMetaData(ClassMetaData meta) {

    }

}
