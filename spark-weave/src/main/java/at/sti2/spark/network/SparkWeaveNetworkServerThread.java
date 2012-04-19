/*
 * Copyright (c) 2012, University of Innsbruck, Austria.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package at.sti2.spark.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;

import at.sti2.spark.core.stream.Triple;
import at.sti2.spark.core.triple.RDFLiteral;
import at.sti2.spark.core.triple.RDFTriple;
import at.sti2.spark.core.triple.RDFURIReference;
import at.sti2.spark.core.triple.RDFValue;
import at.sti2.spark.epsilon.network.run.Token;
import at.sti2.spark.rete.WorkingMemoryElement;
import at.sti2.spark.rete.alpha.AlphaMemory;

public class SparkWeaveNetworkServerThread extends Thread {

	static Logger logger = Logger
			.getLogger(SparkWeaveNetworkServerThread.class);

	private SparkWeaveNetwork sparkWeaveNetwork = null;
	private Socket socket = null;

	public SparkWeaveNetworkServerThread(SparkWeaveNetwork sparkWeaveNetwork,
			Socket socket) {
		this.sparkWeaveNetwork = sparkWeaveNetwork;
		this.socket = socket;
	}

	
	public void run(){
		
		long tripleCounter = 0;
//		long timepoint = (new Date()).getTime();

		long startProcessingTime;
		long endProcessingTime;

		try {
			BufferedReader streamReader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));

			String tripleLine = null;

			startProcessingTime = (new Date()).getTime();

			while ((tripleLine = streamReader.readLine()) != null) {

				Triple sTriple = new Triple(parseTriple(tripleLine),
						(new Date()).getTime(), false, 0l);
				sparkWeaveNetwork.activateNetwork(sTriple);

				tripleCounter++;
				if(tripleCounter%2==0)
					runGC();

				// if (tripleCounter%1000 == 0){
				// logger.info(sparkWeaveNetwork.getEpsilonNetwork().getNetwork().getEpsilonMemoryLevels());
				// logger.info(sparkWeaveNetwork.getReteNetwork().getWorkingMemory().getAlphaMemoryLevels());
				// logger.info(sparkWeaveNetwork.getReteNetwork().getBetaMemoryLevels());

				// logger.info("Processing " + (1000/(sTriple.getTimestamp() -
				// timepoint)) + " triples/sec.");
				// timepoint = sTriple.getTimestamp();
				// }
			}

			endProcessingTime = new Date().getTime();

			streamReader.close();
			socket.close();

			StringBuffer timeBuffer = new StringBuffer();
			timeBuffer.append("Processing took ["
					+ (endProcessingTime - startProcessingTime) + "ms] ");
			timeBuffer
					.append((endProcessingTime - startProcessingTime) / 60000);
			timeBuffer.append(" min ");
			timeBuffer
					.append(((endProcessingTime - startProcessingTime) % 60000) / 1000);
			timeBuffer.append(" s ");
			timeBuffer.append((endProcessingTime - startProcessingTime) % 1000);
			timeBuffer.append(" ms.");

			logger.info(timeBuffer.toString());
			logger.info("Pattern has been matched "
					+ sparkWeaveNetwork.getReteNetwork().getNumMatches()
					+ " times.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to parse triple in N-Triple format.
	 * 
	 * Note: StringTokenizer is not really helpful because lexical form in
	 * literals can have blank spaces.
	 * 
	 * @param tripleLine
	 * @return
	 */
	private RDFTriple parseTriple(String tripleLine) {

		char tripleChars[] = tripleLine.toCharArray();
		int currentPos = 0;

		// ----------------------------------------------
		// Parse subject RDF node
		// ----------------------------------------------
		while (tripleChars[currentPos] != '<')
			currentPos++;

		// Move one place beyond '<'
		currentPos++;

		// Copy URI value
		StringBuffer buffer = new StringBuffer();
		while (tripleChars[currentPos] != '>') {
			buffer.append(tripleChars[currentPos]);
			currentPos++;
		}
		RDFURIReference tripSubject = new RDFURIReference(buffer.toString());

		// ----------------------------------------------
		// Parse predicate RDF node
		// ----------------------------------------------
		while (tripleChars[currentPos] != '<')
			currentPos++;

		// Move one place beyond '<'
		currentPos++;
		buffer = new StringBuffer();
		while (tripleChars[currentPos] != '>') {
			buffer.append(tripleChars[currentPos]);
			currentPos++;
		}
		RDFURIReference tripPredicate = new RDFURIReference(buffer.toString());

		// ----------------------------------------------
		// Parse object RDF node
		// ----------------------------------------------
		// Move one place beyond '>'
		currentPos++;
		RDFValue tripObject = null;
		String lexicalForm = null;
		String languageTag = null;
		RDFURIReference datatypeURI = null;

		// Search while character under scope is != ' '
		while ((tripleChars[currentPos] != '<')
				&& (tripleChars[currentPos] != '"'))
			currentPos++;

		// The character indicates literal value
		if (tripleChars[currentPos] == '"') {

			// Move one character place beyond "
			currentPos++;

			buffer = new StringBuffer();
			while (tripleChars[currentPos] != '"') {
				buffer.append(tripleChars[currentPos]);
				currentPos++;
			}
			lexicalForm = buffer.toString();

			// Search for the beginning of datatype uri
			while (tripleChars[currentPos] != '<')
				currentPos++;

			// Move one character place beyond <
			currentPos++;

			buffer = new StringBuffer();
			while (tripleChars[currentPos] != '>') {
				buffer.append(tripleChars[currentPos]);
				currentPos++;
			}

			datatypeURI = new RDFURIReference(buffer.toString());

			tripObject = new RDFLiteral(lexicalForm, datatypeURI, languageTag);
			// The character is '<' and we have another URL
		} else {
			// Move one place beyond '<'
			currentPos++;
			buffer = new StringBuffer();
			while (tripleChars[currentPos] != '>') {
				buffer.append(tripleChars[currentPos]);
				currentPos++;
			}
			tripObject = new RDFURIReference(buffer.toString());
		}

		return new RDFTriple(tripSubject, tripPredicate, tripObject);
	}

	public void runGC() {

		/************************************************
		 * CLEANING EPSILON NETWORK
		 ************************************************/

		for (Iterator<Triple> ptIter = sparkWeaveNetwork.getEpsilonNetwork()
				.getProcessedTriples().iterator(); ptIter.hasNext();) {

			Triple processedTriple = ptIter.next();

			for (Token token : sparkWeaveNetwork.getEpsilonNetwork()
					.getTokenNodesByStreamedTriple(processedTriple))
				token.removeTokenFromNode();

			// Remove the list of tokens for given streamed triple
			sparkWeaveNetwork.getEpsilonNetwork().removeListByStreamedTriple(
					processedTriple);

			// Remove the streamed triple from the list
			ptIter.remove();
		}

		/************************************************
		 * CLEANING RETE NETWORK
		 ************************************************/

		// GC wakes up and goes through the list WorkingMemoryElements to clean
		// them up
		// long gcThresholdTimestamp = sparkWeaveNetwork.getLastTimestamp() -
		// sparkWeaveNetwork.getTimeWindowLength();
		long gcThresholdTimestamp = System.currentTimeMillis()
				- sparkWeaveNetwork.getTimeWindowLength();

//		 StringBuffer buffer = new StringBuffer("AM ");

		// Loop over all alpha memories in RETE and check WMEs which they hold
		for (AlphaMemory alphaMemory : sparkWeaveNetwork.getReteNetwork()
				.getWorkingMemory().getAlphaMemories()) {


			for (Iterator<WorkingMemoryElement> wmeIterator = alphaMemory
					.getItems().iterator(); wmeIterator.hasNext();) {

				WorkingMemoryElement wme = wmeIterator.next();

				if (wme.getTriple().getTimestamp() < gcThresholdTimestamp) {

					/**
					 * Here we need to delete all references to the WME: 1.
					 * AlphaMemory which points to this WME 2. Tokens which
					 * point to this WME 3. WorkingMemory list of all WMEs
					 */
					wme.remove();

					// Removing the
					wmeIterator.remove();
				} else {
					break;
				}
			}

//			 buffer.append('[');
//			 buffer.append(alphaMemory.getPermanentItems().size());
//			 buffer.append(',');
//			 buffer.append(alphaMemory.getItems().size());
//			 buffer.append(']');
//			 buffer.append(' ');
		}
//		 buffer.append('\n');
//		 System.out.println(buffer.toString());
//		 System.out.println(sparkWeaveNetwork.getReteNetwork().getBetaMemoryLevels());
	}
}
