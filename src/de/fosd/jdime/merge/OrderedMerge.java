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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.fosd.jdime.artifact.Artifacts.copyTree;
import static de.fosd.jdime.artifact.Artifacts.root;
import static de.fosd.jdime.config.merge.MergeScenario.BASE;

/**
 *
 * This class provides an implementation of an ordered three-way merge.
 *
 * <p>
 * The rules for a structured three-way merge are well documented,<br>
 * e.g., by Bernhard Westfechtel's "Structure-oriented merging of revisions of software documents"<br>
 * (Proceedings of the 3rd international workshop on Software configuration management, ACM, 1991)
 * </p>
 *
 * <p>
 * We documented the (JDime specific) results for each input situation in a table that is available as Google Doc.
 * </p>
 *
 * @see <a href="https://docs.google.com/spreadsheets/d/1LQgR_cTPhH4vFuy-7HLpfa-HF4PmYxsrGzRTs1EHVmk/edit?usp=sharing">GoogleDoc</a>
 *
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
        boolean assertsEnabled = false;
        assert assertsEnabled = true;

        Revision leftRev, baseRev, rightRev;
        Iterator<T> leftIt, rightIt;

        {
            MergeScenario<T> mergeScenario = operation.getMergeScenario();
            T left = mergeScenario.getLeft();
            T base = mergeScenario.getBase();
            T right = mergeScenario.getRight();
            logprefix = operation.getId() + " - ";

            assert (left.matches(right));
            assert (left.hasMatching(right)) && right.hasMatching(left);

            LOG.finest(() -> {
                String name = getClass().getSimpleName();
                return String.format("%s%s.merge(%s, %s, %s)", prefix(), name, left.getId(), base.getId(), right.getId());
            });

            leftRev = left.getRevision();
            baseRev = base.getRevision();
            rightRev = right.getRevision();
            leftIt = left.getChildren().iterator();
            rightIt = right.getChildren().iterator();
        }

        T target = operation.getTarget();

        boolean leftDone = false;
        boolean rightDone = false;
        T leftChild = null;
        T rightChild = null;

        if (leftIt.hasNext()) {
            leftChild = leftIt.next();
        } else {
            leftDone = true;
        }

        if (rightIt.hasNext()) {
            rightChild = rightIt.next();
        } else {
            rightDone = true;
        }

        while (!leftDone && !rightDone) {
            boolean moveLeft = false, moveRight = false;
            boolean lr = leftChild.hasMatching(rightChild);
            boolean lR = leftChild.hasMatching(rightRev);
            boolean lB = leftChild.hasMatching(baseRev);
            boolean lBf = lB && leftChild.getMatching(baseRev).hasFullyMatched();
            boolean rl = rightChild.hasMatching(leftChild);
            boolean rL = rightChild.hasMatching(leftRev);
            boolean rB = rightChild.hasMatching(baseRev);
            boolean rBf = rB && rightChild.getMatching(baseRev).hasFullyMatched();

            assert !leftChild.isMerged() && !rightChild.isMerged() : "Trying to merge already merged child!";

            if (lr && rl) {
                // 1 X X 1 X X
                // Left and right child match.
                // We have to merge them two-way or three-way depending on whether they have a common ancestor in base.

                MergeType mergeType;
                T baseChild;

                if (lB) {
                    // 1 X 1 1 X X
                    mergeType = MergeType.THREEWAY;
                    baseChild = leftChild.getMatching(baseRev).getMatchingArtifact(leftChild);
                } else {
                    // 1 X 0 1 X X
                    mergeType = MergeType.TWOWAY;
                    baseChild = leftChild.createEmptyArtifact(BASE);
                }

                T targetChild = leftChild.copy();
                target.addChild(targetChild);

                MergeScenario<T> childTriple = new MergeScenario<>(mergeType, leftChild, baseChild, rightChild);
                MergeOperation<T> mergeOp = new MergeOperation<>(childTriple, targetChild);
                mergeOp.apply(context);

                moveLeft = true;
                moveRight = true;

                if (assertsEnabled) {
                    leftChild.setMerged();
                    rightChild.setMerged();
                }
            } else {
                // 0 X X 0 X X
                // Left and right child do not match.

                assert !lr && !rl : "Found asymmetric matchings between " + leftChild + " and " + rightChild;

                if (lR) {
                    // 0 1 X 0 X X
                    // Left child has a match in right revision, but not with right child.

                    if (rL) {
                        // 0 1 X 0 1 X
                        // Right child has a match in left revision, but not with left child.
                        // We cannot determine the insertion order, therefore left and right child are in conflict.

                        /* TODO: This is a cross merge situation: Can this really happen in ordered merge?
                         *       Maybe assert false and see whether this is ever triggered. */

                        ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, rightChild, target);
                        conflictOp.apply(context);

                        moveLeft = true;
                        moveRight = true;

                        if (assertsEnabled) {
                            leftChild.setMerged();
                            rightChild.setMerged();
                        }
                    } else {
                        // 0 1 X 0 0 X
                        // Left child has a match in right revision and will be dealt with later on in the loop.
                        // Right child has no match in left revision.
                        // Therefore, right child was either deleted in the left revision,
                        // or is a change introduced by the right revision.

                        if (rB) {
                            // 0 1 X 0 0 1
                            // Deletion or Conflict:
                            // Right child has no match in the left revision, but a match in base revision.
                            // Right child was deleted in the left revision.
                            // Now it depends on whether there are changes in right child's subtree.

                            if (rBf) {
                                // Deletion:
                                // Right child was deleted in the left revision.
                                // It is not included in the merged revision.

                                DeleteOperation<T> deleteOp = new DeleteOperation<>(rightChild, target, rightRev.getName());
                                deleteOp.apply(context);
                            } else {
                                // Deletion/Deletion or Deletion/Insertion conflict:
                                // Right child was deleted in the left revision,
                                // but its subtree was changed in the right revision.
                                // A respective conflict is added to the merged revision.

                                ConflictOperation<T> conflictOp = new ConflictOperation<>(null, rightChild, target, leftRev.getName(), rightRev.getName());
                                conflictOp.apply(context);
                            }

                            moveRight = true;

                            if (assertsEnabled) {
                                rightChild.setMerged();
                            }
                        } else {
                            // 0 1 X 0 0 0
                            // Left child has a match in right revision, and will be dealt with later on in the loop.
                            // Right child has no match in the base revision and no match in the left revision.
                            // Therefore, it was added by the right revision.
                            // It is included in the merged revision.

                            AddOperation<T> addOp = new AddOperation<>(rightChild, target, rightRev.getName());
                            addOp.apply(context);

                            moveRight = true;

                            if (assertsEnabled) {
                                rightChild.setMerged();
                            }
                        }
                    }
                } else {
                    // 0 0 X 0 X X
                    // Left child has no match in the right revision.
                    // Therefore, left child was either deleted in the right revision,
                    // or is a change introduced by the left revision.

                    if (lB) {
                        // 0 0 1 0 X X
                        // Left child has a match in base revision, it was therefore deleted by the right revision.
                        // Now it depends on whether there are changes in left child's subtree
                        // and on the status of right child.

                        if (rL) {
                            // 0 0 1 0 1 X
                            // Left child was deleted in the right revision.
                            // Right child has a match in the left revision, but not with left child.

                            if (lBf) {
                                // Left child was deleted by the right revision and is not included in the merged revision.

                                DeleteOperation<T> deleteOp = new DeleteOperation<>(leftChild, target, leftRev.getName());
                                deleteOp.apply(context);
                            } else {
                                // Deletion/Deletion or Deletion/Insertion conflict:
                                // Left child was deleted in the right revision,
                                // but its subtree was changed in the left revision.
                                // A respective conflict is added to the merged revision.

                                ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, null, target, leftRev.getName(), rightRev.getName());
                                conflictOp.apply(context);
                            }

                            moveLeft = true;

                            if (assertsEnabled) {
                                leftChild.setMerged();
                            }
                        } else {
                            // 0 0 1 0 0 X
                            // Left child was deleted in the right revision.
                            // Right child has no match in the left revision.
                            // It was either deleted in the left revision,
                            // or is a changed introduced by the right revision.

                            if (rB) {
                                // 0 0 1 0 0 1
                                // Left child was deleted in the right revision.
                                // Right child was deleted in the left revision.
                                // Merge result depends on whether the subtrees were changed.

                                if (lBf && rBf) {
                                    // Both children were deleted, their subtrees were not changed..
                                    // The merged revision includes neither of them.

                                    DeleteOperation<T> deleteOp = new DeleteOperation<>(leftChild, target, leftRev.getName());
                                    deleteOp.apply(context);

                                    deleteOp = new DeleteOperation<>(rightChild, target, rightRev.getName());
                                    deleteOp.apply(context);

                                    moveLeft = true;
                                    moveRight = true;

                                    if (assertsEnabled) {
                                        leftChild.setMerged();
                                        rightChild.setMerged();
                                    }
                                } else {
                                    // Deletion/Deletion or Deletion/Insertion conflict:
                                    // Both children were deleted.
                                    // But at least one of their subtrees was changed.

                                    if (lBf)  {
                                        // Right subtree was changed
                                        DeleteOperation<T> deleteOp = new DeleteOperation<>(leftChild, target, leftRev.getName());
                                        deleteOp.apply(context);

                                        ConflictOperation<T> conflictOp = new ConflictOperation<>(null, rightChild, target, leftRev.getName(), rightRev.getName());
                                        conflictOp.apply(context);
                                    } else if (rBf) {
                                        // Left subtree was changed
                                        DeleteOperation<T> deleteOp = new DeleteOperation<>(rightChild, target, rightRev.getName());
                                        deleteOp.apply(context);

                                        ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, null, target, leftRev.getName(), rightRev.getName());
                                        conflictOp.apply(context);
                                    } else {
                                        // Both subtrees were changed.
                                        ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, rightChild, target, leftRev.getName(), rightRev.getName());
                                        conflictOp.apply(context);
                                    }

                                    moveLeft = true;
                                    moveRight = true;

                                    if (assertsEnabled) {
                                        leftChild.setMerged();
                                        rightChild.setMerged();
                                    }
                                }
                            } else {
                                // 0 0 1 0 0 0
                                // Left child was deleted in the right revision.
                                // Right child has no match in the left revision and no match in the base revision.
                                // Therefore, right child is a change introduced by the right revision.

                                if (lBf) {
                                    // Left child was deleted.
                                    // Right child was added.
                                    // The merged revision will include right child.

                                    AddOperation<T> addOp = new AddOperation<>(rightChild, target, rightRev.getName());
                                    addOp.apply(context);

                                    moveRight = true;

                                    if (assertsEnabled) {
                                        rightChild.setMerged();
                                    }

                                    DeleteOperation<T> deleteOp = new DeleteOperation<>(leftChild, target, leftRev.getName());
                                    deleteOp.apply(context);
                                } else {
                                    // Deletion/Deletion or Deletion/Insertion: conflict.
                                    // Left child was deleted by the right revision, but its subtree was changed by
                                    // the left revision.
                                    // Right child was added.
                                    // A respective conflict is included in the merged revision.

                                    ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, rightChild, target, leftRev.getName(), rightRev.getName());
                                    conflictOp.apply(context);
                                }

                                moveLeft = true;
                                moveRight = true;

                                if (assertsEnabled) {
                                    leftChild.setMerged();
                                    rightChild.setMerged();
                                }
                            }
                        }
                    } else {
                        // 0 0 0 0 X X
                        // Left child has no match in the right revision and no match in the base revision.
                        // Therefore, it was added by the left revision.

                        if (rL) {
                            // 0 0 0 0 1 X
                            // Left child was added and is included in the merged revision.
                            // Right child has a match in the left revision and will be dealt with later on in the loop.

                            AddOperation<T> addOp = new AddOperation<>(leftChild, target, leftRev.getName());
                            addOp.apply(context);

                            moveLeft = true;

                            if (assertsEnabled) {
                                leftChild.setMerged();
                            }
                        } else {
                            // 0 0 0 0 0 X
                            // Left child was added.
                            // Right child has no match in the left revision.
                            // It was either deleted by the left revision or is a change introduced by the right revision.
                            if (rB) {
                                // 0 0 0 0 0 1
                                // Right child was deleted in the left revision.
                                if (rBf) {
                                    // Left child was added and is included in the merged revision.
                                    // Right child was deleted is not included in the merged revision.

                                    AddOperation<T> addOp = new AddOperation<>(leftChild, target, leftRev.getName());
                                    addOp.apply(context);

                                    moveLeft = true;

                                    if (assertsEnabled) {
                                        leftChild.setMerged();
                                    }

                                    DeleteOperation<T> deleteOp = new DeleteOperation<>(rightChild, target, rightRev.getName());
                                    deleteOp.apply(context);
                                } else {
                                    // Deletion/Deletion or Deletion/Insertion conflict:
                                    // Left child was added.
                                    // Right child was deleted in the left revision,
                                    // but its subtree was changed by the right revision
                                    // The merged revision includes a respective conflict.

                                    ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, rightChild, target, leftRev.getName(), rightRev.getName());
                                    conflictOp.apply(context);
                                }

                                moveLeft = true;
                                moveRight = true;

                                if (assertsEnabled) {
                                    leftChild.setMerged();
                                    rightChild.setMerged();
                                }
                            } else {
                                // 0 0 0 0 0 0
                                // Insertion/Insertion Conflict:
                                // Left child was added
                                // Right child was added.
                                // As the merge is ordered, the insertion order is important but cannot be determined.
                                // The merged revision includes a respective conflict.

                                ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, rightChild, target);
                                conflictOp.apply(context);

                                moveLeft = true;
                                moveRight = true;

                                if (assertsEnabled) {
                                    leftChild.setMerged();
                                    rightChild.setMerged();
                                }
                            }
                        }
                    }
                }
            }

            if (moveLeft) {
                assert leftChild.isMerged() : "Trying to move past unmerged child " + leftChild.getId();

                if (leftIt.hasNext()) {
                    leftChild = leftIt.next();
                } else {
                    leftChild = null;
                    leftDone = true;
                }
            }

            if (moveRight) {
                assert rightChild.isMerged() : "Trying to move past unmerged child " + rightChild.getId();

                if (rightIt.hasNext()) {
                    rightChild = rightIt.next();
                } else {
                    rightChild = null;
                    rightDone = true;
                }
            }
        }

        while (!leftDone) {
            boolean lB = leftChild.hasMatching(baseRev);
            boolean lBf = lB && leftChild.getMatching(baseRev).hasFullyMatched();

            if (lB) {
                if (lBf) {
                    // LeftChild was deleted in Right.

                    DeleteOperation<T> deleteOp = new DeleteOperation<>(leftChild, target, leftRev.getName());
                    deleteOp.apply(context);
                } else {
                    // Deletion/Deletion conflict.

                    ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, null, target, leftRev.getName(), rightRev.getName());
                    conflictOp.apply(context);
                }
            } else {
                // LeftChild was added.

                AddOperation<T> addOp = new AddOperation<>(leftChild, target, leftRev.getName());
                addOp.apply(context);
            }

            if (assertsEnabled) {
                leftChild.setMerged();
            }

            if (leftIt.hasNext()) {
                leftChild = leftIt.next();
            } else {
                leftDone = true;
            }
        }

        while (!rightDone) {
            boolean rB = rightChild.hasMatching(baseRev);
            boolean rBf = rB && rightChild.getMatching(baseRev).hasFullyMatched();

            if (rB) {
                if (rBf) {
                    // RightChild was deleted in Left.

                    DeleteOperation<T> deleteOp = new DeleteOperation<>(rightChild, target, rightRev.getName());
                    deleteOp.apply(context);
                } else {
                    // Deletion/Deletion conflict.

                    ConflictOperation<T> conflictOp = new ConflictOperation<>(null, rightChild, target, leftRev.getName(), rightRev.getName());
                    conflictOp.apply(context);
                }
            } else {
                // RightChild was added.

                AddOperation<T> addOp = new AddOperation<>(rightChild, target, rightRev.getName());
                addOp.apply(context);
            }

            if (assertsEnabled) {
                rightChild.setMerged();
            }

            if (rightIt.hasNext()) {
                rightChild = rightIt.next();
            } else {
                rightDone = true;
            }
        }

        if (assertsEnabled) {
            MergeScenario<T> mergeScenario = operation.getMergeScenario();
            T left = mergeScenario.getLeft();
            T right = mergeScenario.getRight();

            for (T child : left.getChildren()) {
                assert (child.isMerged()) : "Child was not merged: " + child.getId();
            }
            for (T child : right.getChildren()) {
                assert (child.isMerged()) : "Child was not merged: " + child.getId();
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
