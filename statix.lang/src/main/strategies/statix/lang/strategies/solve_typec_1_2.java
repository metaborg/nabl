package statix.lang.strategies;

import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Map;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.ImmutableRelationDescription;
import mb.nabl2.relations.RelationDescription.Reflexivity;
import mb.nabl2.relations.RelationDescription.Symmetry;
import mb.nabl2.relations.RelationDescription.Transitivity;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.SymmetryException;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class solve_typec_1_2 extends Strategy {

    private static final ILogger log = LoggerUtils.logger(solve_typec_1_2.class);

    public static final Strategy instance = new solve_typec_1_2();

    @Override public IStrategoTerm invoke(final Context context, final IStrategoTerm current, final Strategy opsStr,
            final IStrategoTerm factsTerm, final IStrategoTerm goalsTerm) {
        final ITermFactory TF = context.getFactory();

        final Ops ops = new Ops(context, opsStr);

        final List<Subtype> facts = buildRelation(factsTerm);
        final List<Subtype> goals = buildRelation(goalsTerm);

        final List<Message> errors = Lists.newArrayList();

        final IRelation.Immutable<IStrategoTerm> sub = buildSubtypeRelation(facts, ops, errors);
        Map.Immutable<Tuple2<IStrategoTerm, String>, List<IStrategoTerm>> downcasts =
                buildDowncastRelation(sub, ops, errors);

        final Map.Immutable<IStrategoTerm, IStrategoTerm> bounds =
                solveSubtypeConstraints(sub, downcasts, goals, ops, errors);

        final IStrategoTerm[] errorTerms = errors.stream().map(m -> m.toTerm(TF)).toArray(IStrategoTerm[]::new);

        return TF.makeTuple(TF.makeList(), TF.makeList(errorTerms));
    }

    private IRelation.Immutable<IStrategoTerm> buildSubtypeRelation(final List<Subtype> facts, Ops ops,
            List<Message> errors) {
        final IRelation.Transient<IStrategoTerm> sub = Relation.Transient.of(ImmutableRelationDescription
                .of(Reflexivity.REFLEXIVE, Symmetry.ANTI_SYMMETRIC/*NON_SYMMETRIC*/, Transitivity.TRANSITIVE));
        for(Subtype entry : facts) {
            if(ops.isVar(entry.type1) || ops.isVar(entry.type2)) {
                throw new java.lang.IllegalArgumentException("Expect ground types in subtype relation, got " + entry);
            }
            try {
                sub.add(entry.type1, entry.type2);
            } catch(SymmetryException ex) {
                errors.add(entry.makeMessage("Injection cycle not allowed."));
            } catch(RelationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return sub.freeze();
    }

    private Map.Immutable<Tuple2<IStrategoTerm, String>, List<IStrategoTerm>>
            buildDowncastRelation(final IRelation.Immutable<IStrategoTerm> sub, Ops ops, List<Message> errors) {
        Map.Transient<Tuple2<IStrategoTerm, String>, List<IStrategoTerm>> downcasts = Map.Transient.of();
        for(IStrategoTerm type : sub.entries().valueSet()) {
            for(IStrategoTerm subtype : sub.smaller(type)) {
                final Tuple2<String, List<IStrategoTerm>> decomp;
                if((decomp = ops.decompose(subtype).orElse(null)) != null) {
                    final Tuple2<IStrategoTerm, String> key = ImmutableTuple2.of(type, decomp._1());
                    if(downcasts.containsKey(key)) {
                        log.error("Overloaded injection of {} into {}", decomp._1(), ops.pp(type));
                        // TODO actual error
                    } else {
                        downcasts.__put(key, decomp._2());
                    }
                }
            }
        }
        return downcasts.freeze();
    }

    private Map.Immutable<IStrategoTerm, IStrategoTerm> solveSubtypeConstraints(IRelation.Immutable<IStrategoTerm> sub,
            Map.Immutable<Tuple2<IStrategoTerm, String>, List<IStrategoTerm>> downcasts, List<Subtype> goals, Ops ops,
            List<Message> errors) {
        final Map.Transient<IStrategoTerm, IStrategoTerm> bounds = Map.Transient.of();
        // TODO Topologically sort constraints, so bounds for variables that appear on the lhs of constraints
        //      are found (and finished!) before those constraints are solved.
        final Deque<Subtype> worklist = Lists.newLinkedList(goals);
        while(!worklist.isEmpty()) {
            final Subtype entry = worklist.pop();
            log.info("solve {}", entry.toString(ops::pp));
            final boolean isVar1 = ops.isVar(entry.type1);
            final boolean isVar2 = ops.isVar(entry.type2);
            if(isVar1 && isVar2) {
                throw new IllegalArgumentException();
            } else if(isVar1) {
                log.warn("TODO upper bound");
            } else if(isVar2) {
                log.warn("TODO lower bound");
            } else {
                final Optional<Tuple2<String, List<IStrategoTerm>>> decomp1 = ops.decompose(entry.type1);
                final Optional<Tuple2<String, List<IStrategoTerm>>> decomp2 = ops.decompose(entry.type2);
                if(decomp1.isPresent() && decomp2.isPresent()) {
                    final String kind1 = decomp1.get()._1();
                    final String kind2 = decomp2.get()._1();
                    if(!kind1.equals(kind2)) {
                        errors.add(
                                entry.makeMessage(ops.pp(entry.type1) + " incompatible with " + ops.pp(entry.type2)));
                    } else {
                        final List<IStrategoTerm> args1 = decomp1.get()._2();
                        final List<IStrategoTerm> args2 = decomp2.get()._2();
                        for(int i = 0; i < args1.size(); i++) {
                            worklist.push(new Subtype(args1.get(i), args2.get(i), entry.origin));
                        }
                    }
                } else if(decomp1.isPresent()) {
                    final String kind1 = decomp1.get()._1();
                    final Tuple2<IStrategoTerm, String> key2 = ImmutableTuple2.of(entry.type2, kind1);
                    if(downcasts.containsKey(key2)) {
                        final List<IStrategoTerm> args1 = decomp1.get()._2();
                        final List<IStrategoTerm> args2 = downcasts.get(key2);
                        for(int i = 0; i < args1.size(); i++) {
                            worklist.push(new Subtype(args1.get(i), args2.get(i), entry.origin));
                        }
                    } else {
                        errors.add(
                                entry.makeMessage(ops.pp(entry.type1) + " incompatible with " + ops.pp(entry.type2)));
                    }
                } else if(decomp2.isPresent()) {
                    errors.add(entry.makeMessage(ops.pp(entry.type1) + " incompatible with " + ops.pp(entry.type2)));
                } else {
                    if(!sub.contains(entry.type1, entry.type2)) {
                        errors.add(entry.makeMessage(ops.pp(entry.type1) + " not a subtype of " + ops.pp(entry.type2)));
                    }
                }

            }


            //            if(ops.isVar(entry.type2)) {
            //                // TODO decomp1 is present
            //                if(bounds.containsKey(entry.type2)) {
            //                    final IStrategoTerm prevTy = bounds.get(entry.type2);
            //                    final IStrategoTerm newTy;
            //                    if((newTy = sub.leastUpperBound(entry.type1, prevTy).orElse(null)) != null) {
            //                        bounds.__put(entry.type2, newTy);
            //                    } else {
            //                        errors.add(entry.makeMessage(
            //                                ops.pp(entry.type1) + " has no common upper bound with " + ops.pp(prevTy)));
            //                    }
            //                } else {
            //                    final IStrategoTerm newTy = entry.type1;
            //                    bounds.__put(entry.type2, newTy);
            //                }
            //            }

        }

        return bounds.freeze();
    }

    private List<Subtype> buildRelation(IStrategoTerm pairs) {
        if(!TermUtils.isList(pairs)) {
            throw new java.lang.IllegalArgumentException("Expected list of pairs, got " + pairs);
        }
        List<Subtype> rel = Lists.newArrayList();
        for(IStrategoTerm pair : pairs.getAllSubterms()) {
            if(!TermUtils.isTuple(pair, 3)) {
                throw new java.lang.IllegalArgumentException("Expected pair, got " + pair);
            }
            rel.add(new Subtype(pair.getSubterm(0), pair.getSubterm(1), pair.getSubterm(2)));
        }
        return rel;
    }

    private static class Ops {

        private final Context context;
        private final Strategy opsStr;

        private final ITermFactory TF;

        public Ops(Context context, Strategy opsStr) {
            this.context = context;
            this.opsStr = opsStr;
            this.TF = context.getFactory();
        }

        public boolean isVar(IStrategoTerm type) {
            final IStrategoTerm input = TF.makeTuple(TF.makeString("is-var"), type);
            return opsStr.invoke(context, input) != null;
        }

        public Optional<Tuple2<String, List<IStrategoTerm>>> decompose(IStrategoTerm type) {
            final IStrategoTerm input = TF.makeTuple(TF.makeString("decompose"), type);
            final IStrategoTerm output = opsStr.invoke(context, input);
            return Optional.ofNullable(output).map(o -> {
                final String kind = TermUtils.asJavaStringAt(o, 0).orElseThrow(() -> new IllegalArgumentException());
                final List<IStrategoTerm> args = TermUtils.toJavaListAt(o, 1);
                return ImmutableTuple2.of(kind, args);
            });
        }

        public String pp(IStrategoTerm type) {
            final IStrategoTerm input = TF.makeTuple(TF.makeString("pp"), type);
            final IStrategoTerm output = opsStr.invoke(context, input);
            return Optional.ofNullable(output).flatMap(TermUtils::asJavaString).orElseGet(() -> type.toString());

        }

    }

    private static class Subtype {

        public final IStrategoTerm type1;
        public final IStrategoTerm type2;
        public final IStrategoTerm origin;

        public Subtype(IStrategoTerm type1, IStrategoTerm type2, IStrategoTerm origin) {
            this.type1 = type1;
            this.type2 = type2;
            this.origin = origin;
        }

        public Message makeMessage(String msg) {
            return new Message(origin, msg);
        }

        public String toString(Function1<IStrategoTerm, String> pp) {
            return pp.apply(type1) + " <: " + pp.apply(type2);
        }

        @Override public String toString() {
            return type1 + " <: " + type2;
        }

    }

    private static class Message {

        public final IStrategoTerm origin;
        public final String message;

        public Message(IStrategoTerm origin, String message) {
            this.message = message;
            this.origin = origin;
        }

        public IStrategoTerm toTerm(ITermFactory TF) {
            return TF.makeTuple(origin, TF.makeString(message));
        }

    }

}