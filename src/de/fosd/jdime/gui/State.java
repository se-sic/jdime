package de.fosd.jdime.gui;

/**
 * A Bean encapsulating the state of the gui at one point.
 */
class State {

	private String output;
	private String left;
	private String base;
	private String right;
	private String jDime;
	private String cmdArgs;

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
