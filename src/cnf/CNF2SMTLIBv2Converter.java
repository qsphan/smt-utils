package cnf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;

public class CNF2SMTLIBv2Converter {

	private HashSet<Integer> vars;
	private int nVars; // number of variables
	private int nClauses; // number of clauses declared by p

	private int numberOfClauses; // as parsed from file

	public CNF2SMTLIBv2Converter() {
		vars = new HashSet<Integer>();
		nVars = 0;
		nClauses = 0;
		numberOfClauses = 0;
	}

	private String convertLiteral(String lit) {
		int nlit = Integer.parseInt(lit);

		if (nlit == 0)
			return "";

		int atom = Math.abs(nlit);
		vars.add(atom);
		if (nlit == 0)
			return "";
		if (nlit < 0)
			return "( not p" + atom + " )";
		else
			return "p" + atom;
	}

	/*
	 * Convert a clause in CNF to a clause in SMTLIBv2
	 */
	private String convertClause(String cnf) {
		String smt = " ( or ";
		String lits[] = cnf.trim().split("\\s+");
		for (String lit : lits) {
			smt = smt + convertLiteral(lit) + " ";
		}
		smt = smt + ")\n";
		return smt;
	}

	/*
	 * check if the number of variables and clauses are actually the same as
	 * declaration
	 */
	public void check() throws CNFException {

		if (!(nVars == vars.size())) {
			throw new CNFException("Incorrect number of variables");
		}

		if (!(nClauses == numberOfClauses)) {
			throw new CNFException("Incorrect number of clauses");
		}
	}

	public String convertDimacs(String cnf, String smtlib2) {

		String smt2 = null;

		try (BufferedReader br = new BufferedReader(new FileReader(cnf))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			sb.append("( assert ( and \n");

			boolean extendToNextLine = false;
			String currentClause = "";

			while (line != null) {

				if (line.length() == 0) {
					// do nothing for empty line
					line = br.readLine();
					continue;
				}

				String str[] = line.split("\\s+");

				switch (str[0]) {
				case "c":
					// this line is a comment, do nothing
					break;
				case "p":
					nVars = Integer.parseInt(str[2]);
					nClauses = Integer.parseInt(str[3]);
					break;
				default:
					if (extendToNextLine) {
						currentClause = currentClause + " " + line;
						if (str[str.length - 1].equals("0")) {
							// this line is the end of a clause
							sb.append(convertClause(currentClause));
							numberOfClauses++;
							// reset
							extendToNextLine = false;
							currentClause = "";
						}
					} else {
						// this line is a new clause
						if (str[str.length - 1].equals("0")) {
							// the clause finishes in this line
							sb.append(convertClause(line));
							numberOfClauses++;
						} else {
							extendToNextLine = true;
							currentClause = currentClause + " " + line;
						}
					}

				}

				line = br.readLine();
			}

			sb.append("))\n");
			sb.append("\n( check-sat )");
			sb.append("\n( get-model )");

			check();

			String formula = sb.toString();

			writeToFile(smtlib2, getSMTLIB2Info() + getDeclaration() + formula);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return smt2;
	}

	private String getSMTLIB2Info() {
		String str = "";
		str += "( set-option :produce-models true )\n";
		str += "( set-logic QF_UF )\n\n";
		return str;
	}

	private String getDeclaration() {
		StringBuilder sb = new StringBuilder();
		Iterator<Integer> iter = vars.iterator();
		while (iter.hasNext()) {
			int var = iter.next();
			String decl = "( declare-fun p" + var + " () Bool )\n";
			sb.append(decl);
		}
		sb.append("\n");
		return sb.toString();
	}

	private void writeToFile(String filename, String content) {
		Writer writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "utf-8"));
			writer.write(content);
		} catch (IOException ex) {
			// report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
			}
		}
	}

	public static void main(String[] args) {
		CNF2SMTLIBv2Converter converter = new CNF2SMTLIBv2Converter();
		converter.convertDimacs("test/test.cnf", "test/test.smt2");
	}
}
