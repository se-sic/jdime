package de.fosd.jdime.strategy;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import de.fosd.jdime.common.Artifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.common.MergeScenario;
import de.fosd.jdime.common.operations.MergeOperation;
import de.fosd.jdime.merge.Merge;

public abstract class AbstractNodeStrategy<T extends Artifact<T>> extends MergeStrategy<T> {

	private static final Logger LOG = Logger.getLogger(AbstractNodeStrategy.class.getCanonicalName());

	private Merge<T> merge = new Merge<>();

	/**
	 * TODO: high-level documentation
	 * @param operation the <code>MergeOperation</code> to perform
	 * @param context the <code>MergeContext</code>
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	public final void merge(MergeOperation<T> operation, MergeContext context) throws IOException,
			InterruptedException {
		assert (operation != null);
		assert (context != null);

		MergeScenario<T> triple = operation.getMergeScenario();

		assert (triple.isValid());

		T left = triple.getLeft();
		T base = triple.getBase();
		T right = triple.getRight();
		T target = operation.getTarget();

		Arrays.asList(left, base, right).forEach(t -> {
			assert t.exists();
		});

		assert (target != null);

		LOG.finest(() -> String.format("Merging using operation %s and context %s", operation, context));
		merge.merge(operation, context);
	}
}
