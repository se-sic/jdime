/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 */
package de.fosd.jdime.merge;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.MergeType;
import de.fosd.jdime.common.Revision;
import de.fosd.jdime.common.operations.AddOperation;
import de.fosd.jdime.common.operations.ConflictOperation;
import de.fosd.jdime.common.operations.DeleteOperation;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.matcher.Matching;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class OrderedMerge<T extends Artifact<T>> implements MergeInterface<T> {

    private static final Logger LOG = Logger.getLogger(OrderedMerge.class.getCanonicalName());
    private String logprefix;

    /**
     * TODO: this needs high-level documentation. Probably also detailed documentation.
     *
     * @param operation the <code>MergeOperation</code> to perform
     * @param context the <code>MergeContext</code>
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public final void merge(final MergeOperation<T> operation,
            final MergeContext context) throws IOException,
            InterruptedException {

        MergeScenario<T> triple = operation.getMergeScenario();
        T left = triple.getLeft();
        T base = triple.getBase();
        T right = triple.getRight();
        T target = operation.getTarget();
        logprefix = operation.getId() + " - ";

        assert (left.matches(right));
        assert (left.hasMatching(right)) && right.hasMatching(left);

        LOG.finest(() -> {
            String name = getClass().getSimpleName();
            return String.format("%s%s.merge(%s, %s, %s)", prefix(), name, left.getId(), base.getId(), right.getId());
        });

        Revision l = left.getRevision();
        Revision b = base.getRevision();
        Revision r = right.getRevision();
        Iterator<T> leftIt = left.getChildren().iterator();
        Iterator<T> rightIt = right.getChildren().iterator();

        boolean leftdone = false;
        boolean rightdone = false;
        T leftChild = null;
        T rightChild = null;

        if (leftIt.hasNext()) {
            leftChild = leftIt.next();
        } else {
            leftdone = true;
        }
        if (rightIt.hasNext()) {
            rightChild = rightIt.next();
        } else {
            rightdone = true;
        }

        while (!leftdone || !rightdone) {
            if (!leftdone && !r.contains(leftChild)) {
                assert (leftChild != null);
                final T finalLeftChild = leftChild;
                final T finalRightChild = rightChild;

                LOG.finest(() -> String.format("%s is not in right", prefix(finalLeftChild)));

                if (b != null && b.contains(leftChild)) {
                    LOG.finest(() -> String.format("%s was deleted by right", prefix(finalLeftChild)));

                    // was deleted in right
                    if (leftChild.hasChanges()) {
                        // insertion-deletion-conflict
                        if (LOG.isLoggable(Level.FINEST)) {
                            LOG.finest(prefix(leftChild) + "has changes in subtree.");
                        }
                        ConflictOperation<T> conflictOp = new ConflictOperation<>(
                                leftChild, rightChild, target, l.getName(), r.getName());
                        conflictOp.apply(context);
                        if (rightIt.hasNext()) {
                            rightChild = rightIt.next();
                        } else {
                            rightdone = true;
                        }
                        if (leftIt.hasNext()) {
                            leftChild = leftIt.next();
                        } else {
                            leftdone = true;
                        }
                    } else {
                        // can be safely deleted
                        DeleteOperation<T> delOp = new DeleteOperation<>(leftChild, target, triple, l.getName());
                        delOp.apply(context);
                    }
                } else {
                    LOG.finest(() -> String.format("%s is a change", prefix(finalLeftChild)));

                    // leftChild is a change
                    if (!rightdone && !l.contains(rightChild)) {
                        assert (rightChild != null);
                        LOG.finest(() -> String.format("%s is not in left", prefix(finalRightChild)));

                        if (b != null && b.contains(rightChild)) {
                            LOG.finest(() -> String.format("%s was deleted by left", prefix(finalRightChild)));

                            // rightChild was deleted in left
                            if (rightChild.hasChanges()) {
                                LOG.finest(() -> String.format("%s has changes in subtree.", prefix(finalRightChild)));

                                // deletion-insertion conflict
                                ConflictOperation<T> conflictOp = new ConflictOperation<>(
                                        leftChild, rightChild, target, l.getName(), r.getName());
                                conflictOp.apply(context);
                                if (rightIt.hasNext()) {
                                    rightChild = rightIt.next();
                                } else {
                                    rightdone = true;
                                }
                                if (leftIt.hasNext()) {
                                    leftChild = leftIt.next();
                                } else {
                                    leftdone = true;
                                }
                            } else {
                                // add the left change
                                AddOperation<T> addOp = new AddOperation<>(leftChild, target, triple, l.getName());
                                leftChild.setMerged();
                                addOp.apply(context);
                            }
                        } else {
                            LOG.finest(() -> String.format("%s is a change", prefix(finalRightChild)));

                            // rightChild is a change
                            ConflictOperation<T> conflictOp = new ConflictOperation<>(
                                    leftChild, rightChild, target, l.getName(), r.getName());
                            conflictOp.apply(context);

                            if (rightIt.hasNext()) {
                                rightChild = rightIt.next();
                            } else {
                                rightdone = true;
                            }
                        }
                    } else {
                        LOG.finest(() -> String.format("%s adding change", prefix(finalLeftChild)));

                        // add the left change
                        AddOperation<T> addOp = new AddOperation<>(leftChild, target, triple, l.getName());
                        leftChild.setMerged();
                        addOp.apply(context);
                    }
                }

                if (leftIt.hasNext()) {
                    leftChild = leftIt.next();
                } else {
                    leftdone = true;
                }
            }

            if (!rightdone && !l.contains(rightChild)) {
                assert (rightChild != null);
                final T finalLeftChild = leftChild;
                final T finalRightChild = rightChild;

                LOG.finest(() -> String.format("%s is not in left", prefix(finalRightChild)));

                if (b != null && b.contains(rightChild)) {
                    LOG.finest(() -> String.format("%s was deleted by left", prefix(finalRightChild)));

                    // was deleted in left
                    if (rightChild.hasChanges()) {
                        LOG.finest(() -> String.format("%s has changes in subtree.", prefix(finalRightChild)));

                        // insertion-deletion-conflict
                        ConflictOperation<T> conflictOp = new ConflictOperation<>(
                                leftChild, rightChild, target, l.getName(), r.getName());
                        conflictOp.apply(context);
                        if (rightIt.hasNext()) {
                            rightChild = rightIt.next();
                        } else {
                            rightdone = true;
                        }
                        if (leftIt.hasNext()) {
                            leftChild = leftIt.next();
                        } else {
                            leftdone = true;
                        }
                    } else {
                        // can be safely deleted
                        DeleteOperation<T> delOp = new DeleteOperation<>(rightChild, target, triple, r.getName());
                        delOp.apply(context);
                    }
                } else {
                    LOG.finest(() -> String.format("%s is a change", prefix(finalRightChild)));

                    // rightChild is a change
                    if (!leftdone && !r.contains(leftChild)) {
                        assert (leftChild != null);
                        LOG.finest(() -> String.format("%s is not in right", prefix(finalLeftChild)));

                        if (b != null && b.contains(leftChild)) {
                            LOG.finest(() -> String.format("%s was deleted by right", prefix(finalLeftChild)));

                            if (leftChild.hasChanges()) {
                                LOG.finest(() -> String.format("%s has changes in subtree", prefix(finalLeftChild)));

                                // deletion-insertion conflict
                                ConflictOperation<T> conflictOp = new ConflictOperation<>(
                                        leftChild, rightChild, target, l.getName(), r.getName());
                                conflictOp.apply(context);
                                if (rightIt.hasNext()) {
                                    rightChild = rightIt.next();
                                } else {
                                    rightdone = true;
                                }
                                if (leftIt.hasNext()) {
                                    leftChild = leftIt.next();
                                } else {
                                    leftdone = true;
                                }
                            } else {
                                LOG.finest(() -> String.format("%s adding change", prefix(finalRightChild)));

                                // add the right change
                                AddOperation<T> addOp = new AddOperation<>(rightChild, target, triple, r.getName());
                                rightChild.setMerged();
                                addOp.apply(context);
                            }
                        } else {
                            LOG.finest(() -> String.format("%s is a change", prefix(finalLeftChild)));

                            // leftChild is a change
                            ConflictOperation<T> conflictOp = new ConflictOperation<>(
                                    leftChild, rightChild, target, l.getName(), r.getName());
                            conflictOp.apply(context);

                            if (leftIt.hasNext()) {
                                leftChild = leftIt.next();
                            } else {
                                leftdone = true;
                            }
                        }
                    } else {
                        LOG.finest(() -> String.format("%s adding change", prefix(finalRightChild)));

                        // add the right change
                        AddOperation<T> addOp = new AddOperation<>(rightChild, target, triple, r.getName());
                        rightChild.setMerged();
                        addOp.apply(context);
                    }
                }

                if (rightIt.hasNext()) {
                    rightChild = rightIt.next();
                } else {
                    rightdone = true;
                }

            } else if (l.contains(rightChild) && r.contains(leftChild)) {
                assert (leftChild != null);
                assert (rightChild != null);

                // left and right have the artifact. merge it.
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest(prefix(leftChild) + "is in both revisions [" + rightChild.getId() + "]");
                }

                // leftChild is a choice node
                if (leftChild.isChoice()) {
                    T matchedVariant = rightChild.getMatching(l).getMatchingArtifact(rightChild);
                    leftChild.addVariant(r.getName(), matchedVariant);
                    AddOperation<T> addOp = new AddOperation<>(leftChild, target, triple, null);
                    leftChild.setMerged();
                    rightChild.setMerged();
                    addOp.apply(context);
                } else {
                    assert (leftChild.hasMatching(rightChild) && rightChild.hasMatching(leftChild));
                }

                if (!leftChild.isMerged() && !rightChild.isMerged()) {
                    // determine whether the child is 2 or 3-way merged
                    Matching<T> mBase = leftChild.getMatching(b);

                    MergeType childType = mBase == null ? MergeType.TWOWAY
                            : MergeType.THREEWAY;
                    T baseChild = mBase == null ? leftChild.createEmptyArtifact()
                            : mBase.getMatchingArtifact(leftChild);
                    T targetChild = target == null ? null : target.addChild(leftChild.clone());
                    if (targetChild != null) {
                        assert targetChild.exists();
                        targetChild.deleteChildren();
                    }

                    MergeScenario<T> childTriple = new MergeScenario<>(childType,
                            leftChild, baseChild, rightChild);

                    MergeOperation<T> mergeOp = new MergeOperation<>(childTriple, targetChild, l.getName(), r.getName());

                    leftChild.setMerged();
                    rightChild.setMerged();
                    mergeOp.apply(context);
                }

                if (leftIt.hasNext()) {
                    leftChild = leftIt.next();
                } else {
                    leftdone = true;
                }

                if (rightIt.hasNext()) {
                    rightChild = rightIt.next();
                } else {
                    rightdone = true;
                }
            }
            if (LOG.isLoggable(Level.FINEST) && target != null) {
                LOG.finest(String.format("%s target.dumpTree() after processing child:", prefix()));
                System.out.println(target.dumpRootTree());
            }
        }
    }

    /**
     * Returns the logging prefix.
     *
     * @return logging prefix
     */
    private String prefix() {
        return logprefix;
    }

    /**
     * Returns the logging prefix.
     *
     * @param artifact
     *            artifact that is subject of the logging
     * @return logging prefix
     */
    private String prefix(T artifact) {
        return String.format("%s[%s]", logprefix, (artifact == null) ? "null" : artifact.getId());
    }
}
