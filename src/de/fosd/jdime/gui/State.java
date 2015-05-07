package de.fosd.jdime.gui;

import java.util.Objects;

/**
 * A Bean encapsulating the state of the gui at one point.
 */
final class State {

	private String output;
	private String left;
	private String base;
	private String right;
	private String jDime;
	private String cmdArgs;

	private State() {

	}

	/**
	 * Returns a <code>State</code> instance containing the current state of the given <code>GUI</code>.
	 *
	 * @param gui
	 * 		the <code>GUI</code> whose state is to be copied
	 *
	 * @return the resulting <code>State</code>
	 */
	public static State of(GUI gui) {
		State state = new State();

		state.output = gui.output.getText();
		state.left = gui.left.getText();
		state.base = gui.base.getText();
		state.right = gui.right.getText();
		state.jDime = gui.jDime.getText();
		state.cmdArgs = gui.cmdArgs.getText();

		return state;
	}

	/**
	 * Applies the state stored in this <code>State</code> to the given <code>GUI</code>. Must be called in the
	 * JavaFX Application thread.
	 *
	 * @param gui
	 * 		the <code>GUI</code> to apply the stored state to
	 */
	public void applyTo(GUI gui) {
		gui.output.setText(output);
		gui.left.setText(left);
		gui.base.setText(base);
		gui.right.setText(right);
		gui.jDime.setText(jDime);
		gui.cmdArgs.setText(cmdArgs);
	}

	public String getOutput() {
		return output;
	}

	public String getLeft() {
		return left;
	}

	public String getBase() {
		return base;
	}

	public String getRight() {
		return right;
	}

	public String getjDime() {
		return jDime;
	}

	public String getCmdArgs() {
		return cmdArgs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		State state = (State) o;

		return Objects.equals(output, state.output) &&
				Objects.equals(left, state.left) &&
				Objects.equals(base, state.base) &&
				Objects.equals(right, state.right) &&
				Objects.equals(jDime, state.jDime) &&
				Objects.equals(cmdArgs, state.cmdArgs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(output, left, base, right, jDime, cmdArgs);
	}
}
