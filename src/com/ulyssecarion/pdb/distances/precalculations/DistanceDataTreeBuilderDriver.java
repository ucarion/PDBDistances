package com.ulyssecarion.pdb.distances.precalculations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.biojava.bio.structure.Element;

import com.ulyssecarion.pdb.distances.DistanceDataTree;
import com.ulyssecarion.pdb.distances.DistanceDataTree.OriginAtomNameTree;
import com.ulyssecarion.pdb.distances.DistanceDataTree.OriginElementTree;
import com.ulyssecarion.pdb.distances.DistanceDataTree.OriginGroupTree;
import com.ulyssecarion.pdb.distances.DistanceDataTree.TargetElementTree;
import com.ulyssecarion.pdb.distances.DistanceDataTree.TargetGroupTree;
import com.ulyssecarion.pdb.distances.DistanceResult;
import com.ulyssecarion.pdb.distances.serialization.DistanceDataTreeSerializer;

public class DistanceDataTreeBuilderDriver {
	private static final int SAVE_EVERY = 1000;
	private static final int START_AT = 26000;

	public static void main(String[] args) throws Exception {
		buildDirectoryFromSavedDataTrees();
	}

	/**
	 * Builds a DistanceDataTree for every 1000 PDB IDs in the database. They
	 * can later be deserialized, joined, and used to find entries by distances.
	 * 
	 * @throws Exception
	 */
	private static void buildAndSaveDataTrees() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("pdbids.txt"));

		List<String> pdbIDs = new ArrayList<>();
		String line;
		while ((line = br.readLine()) != null) {
			pdbIDs.add(line);
		}

		for (int i = START_AT; i < pdbIDs.size(); i += SAVE_EVERY) {
			DistanceDataTree dataTree = new DistanceDataTree();

			System.out.println("Working on block starting with: "
					+ pdbIDs.get(i));

			long start = System.currentTimeMillis();
			for (int j = i; j < i + SAVE_EVERY && j < pdbIDs.size(); j++) {
				System.out.println("Generating table for: " + pdbIDs.get(j)
						+ " (" + j + ")");
				LigandDistanceDataTreeBuilder.buildTreeFor(dataTree,
						pdbIDs.get(j));
			}
			long stop = System.currentTimeMillis();
			System.out.println("That section took " + (stop - start) + " ms.");

			DistanceDataTreeSerializer.serializeDataTree(dataTree,
					pdbIDs.get(i));
		}

		br.close();
	}

	private static void buildDirectoryFromSavedDataTrees() {
		File savedDataTrees = new File(
				DistanceDataTreeSerializer.DDT_OUTPUT_FILE);

		long start = System.currentTimeMillis();
		for (String dataTreeName : savedDataTrees.list()) {
			System.out.println(dataTreeName);

			long startSer = System.currentTimeMillis();
			DistanceDataTree dataTree = DistanceDataTreeSerializer
					.deserializeDataTree(dataTreeName);
			long stopSer = System.currentTimeMillis();
			System.out.println("SER took " + ((stopSer - startSer) / 1000.0));

			buildDirFor(dataTree);
		}
		long stop = System.currentTimeMillis();
		
		System.out.println("Total job took " + ((stop - start) / 1000.0));
	}

	private static void buildDirFor(DistanceDataTree dataTree) {
		long start = System.currentTimeMillis();

		for (String originGroup : dataTree.getOriginGroupNames()) {
			System.out.println("\t" + originGroup);
			buildDirFor(dataTree.get(originGroup).get(0),
					DistanceDataTreeSerializer.DIR_OUTPUT_FOLDER + originGroup
							+ File.separator);
		}

		long stop = System.currentTimeMillis();
		System.out.println("DDT took " + ((stop - start) / 1000.0));
	}

	private static void buildDirFor(OriginGroupTree originGroup, String path) {
		new File(path).mkdir();

		for (Element originElem : originGroup.getKeys()) {
			buildDirFor(originGroup.get(originElem).get(0), path + originElem
					+ File.separator);
		}
	}

	private static void buildDirFor(OriginElementTree originElem, String path) {
		new File(path).mkdir();

		for (String originAtomName : originElem.getKeys()) {
			buildDirFor(originElem.get(originAtomName).get(0), path
					+ originAtomName + File.separator);
		}
	}

	private static void buildDirFor(OriginAtomNameTree originAtom, String path) {
		new File(path).mkdir();

		for (String targetGroup : originAtom.getKeys()) {
			buildDirFor(originAtom.get(targetGroup).get(0), path + targetGroup
					+ File.separator);
		}
	}

	private static void buildDirFor(TargetGroupTree targetGroup, String path) {
		new File(path).mkdir();

		for (Element targetElem : targetGroup.getKeys()) {
			buildDirFor(targetGroup.get(targetElem).get(0), path + targetElem
					+ File.separator);
		}
	}

	private static void buildDirFor(TargetElementTree targetElem, String path) {
		new File(path).mkdir();

		for (String targetAtom : targetElem.getKeys()) {
			buildDirFor(targetElem.get(targetAtom), path + targetAtom
					+ DistanceDataTreeSerializer.EXTENSION);
		}
	}

	private static void buildDirFor(List<DistanceResult> results, String path) {
		if (new File(path).exists()) {
			List<DistanceResult> prev = DistanceDataTreeSerializer
					.deserializeResults(path);
			prev.addAll(results);
			DistanceDataTreeSerializer.serializeResult(prev, path);
		} else {
			DistanceDataTreeSerializer.serializeResult(results, path);
		}
	}
}