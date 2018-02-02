/**
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2017 University of Passau, Germany
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
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
package de.fosd.jdime.merge;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.MergeType;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.operations.AddOperation;
import de.fosd.jdime.operations.ConflictOperation;
import de.fosd.jdime.operations.DeleteOperation;
import de.fosd.jdime.operations.MergeOperation;
import de.fosd.jdime.strdump.DumpMode;

import static de.fosd.jdime.artifact.Artifacts.copyTree;
import static de.fosd.jdime.artifact.Artifacts.root;
import static de.fosd.jdime.config.merge.MergeScenario.BASE;

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
     */
    @Override
    public void merge(MergeOperation<T> operation, MergeContext context) {
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
                    if (leftChild.hasChanges(b)) {
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
                        DeleteOperation<T> delOp = new DeleteOperation<>(leftChild, target, l.getName());
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
                            if (rightChild.hasChanges(b)) {
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
                                AddOperation<T> addOp = new AddOperation<>(copyTree(leftChild), target, l.getName());
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
                        AddOperation<T> addOp = new AddOperation<>(copyTree(leftChild), target, l.getName());
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
                    if (rightChild.hasChanges(b)) {
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
                        DeleteOperation<T> delOp = new DeleteOperation<>(rightChild, target, r.getName());
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

                            if (leftChild.hasChanges(b)) {
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
                                AddOperation<T> addOp = new AddOperation<>(copyTree(rightChild), target, r.getName());
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
                        AddOperation<T> addOp = new AddOperation<>(copyTree(rightChild), target, r.getName());
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
                    AddOperation<T> addOp = new AddOperation<>(leftChild, target, null);
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
                    T baseChild = mBase == null ? leftChild.createEmptyArtifact(BASE)
                            : mBase.getMatchingArtifact(leftChild);

                    T targetChild = leftChild.copy();
                    target.addChild(targetChild);

                    MergeScenario<T> childTriple = new MergeScenario<>(childType,
                            leftChild, baseChild, rightChild);

                    MergeOperation<T> mergeOp = new MergeOperation<>(childTriple, targetChild);

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

            if (!context.isDiffOnly()) {
                LOG.finest(() -> {
                    String dump = root(target).dump(DumpMode.PLAINTEXT_TREE);
                    return String.format("%s target.dumpTree() after processing child:%n%s", prefix(), dump);
                });
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
