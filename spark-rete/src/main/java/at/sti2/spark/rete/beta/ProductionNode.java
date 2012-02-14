/*
 * Copyright (c) 2010, University of Innsbruck, Austria.
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

package at.sti2.spark.rete.beta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import at.sti2.spark.rete.Token;
import at.sti2.spark.rete.WorkingMemoryElement;
import at.sti2.spark.rete.node.RETENode;

public class ProductionNode extends RETENode {
	
	static Logger logger = Logger.getLogger(ProductionNode.class);

	private List <Token> items = null;
	
	long matchCounter = 0;
	
	public ProductionNode(){
		items = Collections.synchronizedList(new ArrayList <Token> ());
	}
	
	public void addItem(Token token){
		synchronized(items){
			items.add(token);
		}
	}
	
	public void removeItem(Token token){
		synchronized(items){
			items.remove(token);
		}
	}
	
	@Override
	public void rightActivate(WorkingMemoryElement wme) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void leftActivate(Token token) {
		
	}

	public long getMatchCounterValue() {
		return matchCounter;
	}

	public void leftActivate(Token token, WorkingMemoryElement wme) {
		
		Token newToken = createToken(token, wme);
		
		//TODO Insert token at the head of items
		addItem(newToken);
		matchCounter++;
		
//		StringBuffer buffer = new StringBuffer();
//		buffer.append("We have " + (++matchCounter) + " match:\n");
//		Token printToken = newToken;
//		
//		while (printToken != null){
//			buffer.append(printToken.getWme().getStreamedTriple().toString());
//			buffer.append('\n');
//			printToken = printToken.getParent();
//		}
//		
//		buffer.append("Time interval [" + newToken.getTimeInterval() + "] ms.");
//		
//		System.out.println(buffer.toString());
		
		//removeResult(newToken);
	}
	
	private Token createToken(Token parentToken, WorkingMemoryElement wme){
		
		Token newToken = new Token();
		newToken.setParent(parentToken);
		newToken.setWme(wme);
		//Added for retraction purposes
		newToken.setNode(this);
		
		//TODO Insert token at the head of parent's children
		if (parentToken!=null){
			parentToken.addChild(newToken);
			
			//Insert initial time interval for the new token
			newToken.setStartTime(parentToken.getStartTime());
			newToken.setEndTime(parentToken.getEndTime());
			
			if (wme.getStreamedTriple().getTimestamp()<newToken.getStartTime())
				newToken.setStartTime(wme.getStreamedTriple().getTimestamp());
			else if (wme.getStreamedTriple().getTimestamp()>newToken.getEndTime())
				newToken.setEndTime(wme.getStreamedTriple().getTimestamp());
			
		} else {
			//Token without parent is token at dummy (root) beta memory
			//It will have start and end time as streamed triple
			newToken.setStartTime(wme.getStreamedTriple().getTimestamp());
			newToken.setEndTime(wme.getStreamedTriple().getTimestamp());
		}
		
		//TODO Insert token at the head of wme tokens
		wme.addToken(newToken);
		
		return newToken;
	}

	public List<Token> getItems() {
		return items;
	}
	
	//The strategy for removing is to climb until the top most token while deleting WME'S from alpha mems
//	private void removeResult(Token resultToken){
//		
//		Token removeToken = resultToken;
//		
//		while(removeToken!=null){
//			
//			//Delete associated WME from alphamem lists
//			//Remove occurrence from each alpha memory
//			for (AlphaMemory alphamem : removeToken.getWme().getAlphaMems())
//				alphamem.removeItem(removeToken.getWme());
//			
//			//Delete the token WME from alpha memories
//			removeToken.getWme().getTokens().remove(removeToken);
//			
//			//Delete the token from the node list
//			if (removeToken.getNode() instanceof BetaMemory)
//				((BetaMemory)removeToken.getNode()).getItems().remove(this);
//			else if (removeToken.getNode() instanceof ProductionNode)
//				((ProductionNode)removeToken.getNode()).getItems().remove(this);
//			
//			//Delete token from the parent list
//			if (removeToken.getParent()!=null)
//				removeToken.getParent().getChildren().remove(this);
//			
//			removeToken = removeToken.getParent();
//		}
//		
//	}
	
	

}