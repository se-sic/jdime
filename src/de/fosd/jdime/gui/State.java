package de.fosd.jdime.gui;

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

	private State() {}

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
}
