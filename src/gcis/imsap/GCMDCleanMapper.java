package gcis.imsap;



import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import Similarity.EntityFeatureModelSimilarity;
import Similarity.Similarity;
import Similarity.Matcher.EntityMatcher;
import Similarity.Matcher.WebOfDataMatcher;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class GCMDCleanMapper {
	
	public static void main(String [] args){

		int level = 2;
//		String onto1 = "../iimb/000/onto.owl";
//		String onto2 = "../iimb/onto.owl";
//		String output = "000.txt";
		
		String onto2 = "scienceKeywords.txt";
		String onto1 = "clean.ttl.txt";
		String output = "GCMDCleanMapping4.html";
		
		Model GCMDModel = ModelFactory.createDefaultModel();
		Model CleanModel = ModelFactory.createDefaultModel();
		BufferedWriter out = null;
		try{
        	FileInputStream fstream = new FileInputStream(onto1);
        	GCMDModel.read(fstream,"","TTL");
        	FileInputStream fstream2 = new FileInputStream(onto2);
        	CleanModel.read(fstream2,"");    
        	
        	FileWriter ofstream = new FileWriter(output);
        	out = new BufferedWriter(ofstream);
        	out.write("<table border =1>");
		}catch(Exception e){
			e.printStackTrace();
		}

		
		Similarity sim = new EntityFeatureModelSimilarity();
		WebOfDataMatcher WDM = new EntityMatcher(GCMDModel,sim,level, 4);
		
		ResIterator subjects = CleanModel.listSubjects();
		Hashtable<String, ArrayList<String>> descriptions = new Hashtable<String, ArrayList<String>>();
		getAllDescriptions(CleanModel,  descriptions);

		Hashtable<String, ArrayList<String>> narrowers = new Hashtable<String, ArrayList<String>>();   		
		narrowers = BroaderVoting.processNarrower(narrowers,GCMDModel);	

		
		while(subjects.hasNext()){
			Resource subject = subjects.next();
			System.out.println("matching... ("+subject.toString()+")");
			
			Hashtable<String, Double> matches = WDM.findMatches(subject.toString(),descriptions);
//			String matching = WDM.findMatch(subject.toString(),descriptions);			
//			ArrayList<String> matches = new ArrayList<String> ();
//			matches.add(matching);
			
			ArrayList<String> keys = sortedKey(matches);
			LinkedHashMap<String, Double> votes = new LinkedHashMap<String, Double>();
			String highestVoteLabel ="";
			for(String match:keys){
				Resource s = ResourceFactory.createResource(match);
				Property p = ResourceFactory.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
				RDFNode v = null;
				
				//StmtIterator itr = CleanModel.listStatements(s, p, v);
				StmtIterator itr = GCMDModel.listStatements(s, p, v);
				String labels = "";
				while(itr.hasNext()){
					 Statement stmt = itr.next();
					 String object = stmt.getObject().toString();
					 labels += object+" ";
				}
				
			    highestVoteLabel = updateVotes(labels.replaceAll("@en", "").trim(),narrowers,votes,matches.get(match));

				
				try{
					//Resource gcmds = ResourceFactory.createResource(subject);
					StmtIterator gitr = CleanModel.listStatements(subject, p, v);
					String glabels = "";
					while(gitr.hasNext()){
						 Statement stmt = gitr.next();
						 String object = stmt.getObject().toString();
						 glabels += object+" ";
					}
					System.out.println("<tr>");
					System.out.println(tableFormat(subject.toString(),match,glabels,matches.get(match)));
					System.out.println("</tr>");
					out.write("<tr>\n");
					out.write(tableFormat(subject.toString(),match,glabels,matches.get(match)));
					
					out.flush();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			try{
			out.write("<td>\n");
		    for(String name:votes.keySet()){
		    	System.out.println(name+": "+votes.get(name));
		    	out.write(name+": "+votes.get(name)+"\n<br/>");
		    }
		    System.out.println(subject.toString()+" match "+highestVoteLabel+" "+votes.get(highestVoteLabel)+"\n");
		    out.write("<b>"+subject.toString()+" broaderMatch "+highestVoteLabel+" "+votes.get(highestVoteLabel)+"</b>\n<br />");
		    out.write("</td>\n");
		    out.write("</tr>\n");
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		try{
			out.write("</table>");
			out.flush();
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static String updateVotes(String myconcept,Hashtable<String, ArrayList<String>> broaders, LinkedHashMap<String, Double> votes, double voteScore ){
		double topVote = 0;
		String topVoteString = "";
		//System.out.println(narrower+" "+narrowers.contains(narrower));
		
//		System.out.println("%"+narrower+"%");
//		for(String key:narrowers.keySet()){
//			if(key.contains("SOLAR")){
//			System.out.println("%"+key+"%:");
//			for(String value:narrowers.get(key))
//				System.out.print(value+", ");
//			System.out.println();
////			try{
////			System.in.read();
////			}catch(Exception e){
////				
////			}
//			}
//		}	
		if(!broaders.keySet().contains(myconcept))
			return topVoteString;
		
		for(String broader:broaders.get(myconcept)){
			if(votes.keySet().contains(broader)){
				double vote = votes.get(broader) + voteScore;

				votes.put(broader, vote);
			}else{
				votes.put(broader, voteScore);
				topVoteString = broader;
			}
		}
		
		for(String concept:votes.keySet()){
			if(votes.get(concept) > topVote){
				topVoteString = concept;
				topVote = votes.get(concept);
			}
		}
		return topVoteString;
	}
	public static String tableFormat(String e1, String e2, String notes,double scores){
//		String output = "<tr>\n";
//		for(String e2:e2s){
		String output ="";
			output += "<td>"+e1+"</td>";
			output += "<td>"+notes+"</td>";
			output += "<td>"+e2+"</td>";
			output += "<td>"+scores+"</td>";
//		}
//		output +="</tr>";
		return output;		
	}
	public static String formatOutput(String e1, String e2, String notes){
		String output = "<match>\n";
		output += "<entity1 rdf:resource=\"";
		output += e1;
		output += "\" />\n";
		output += "<entity2 rdf:resource=\"";
		output += e2;
		output += "\" />\n";
		output += "<Note>";
		output += notes;
		output += "</Note>\n";
		output += "</match>\n";
		return output;
		
	}
	public static void getAllDescriptions(Model model, Hashtable<String, ArrayList<String>> descriptions){

		StmtIterator stmtItr = model.listStatements();
						
		while(stmtItr.hasNext()){
			Statement stmt = stmtItr.next();		
			String stmtString = stmt.toString().replaceAll(",", "").replaceAll("\\[", "").replaceAll("\\]", "");
			String subject = stmt.getSubject().toString();
			
			if(descriptions.keySet().contains(subject)){
				ArrayList<String> statements = descriptions.get(subject);;
				statements.add(stmtString);
				descriptions.put(subject, statements);			
			}else{
				ArrayList<String> statements = new ArrayList<String>();
				statements.add(stmtString);
				descriptions.put(subject, statements);					
			}

		}


	}
	public static String getLiterals(String object, Hashtable<String, ArrayList<String>> descriptions, int level){
		String rStr = "";
		if(!descriptions.containsKey(object)|| level ==0)
			return object;
		
		for(String t:descriptions.get(object)){
//			System.out.println(t);
			String nobject = t.split(" ",3)[2];
			rStr = rStr + " " + getLiterals(nobject, descriptions, level -1);
		}		
		return rStr;
	}
	public static ArrayList<String> sortedKey(Hashtable<String,Double> matches){
		ArrayList<String> keys = new ArrayList<String> (matches.keySet());
		for(int i = 0 ; i < keys.size();i++){
			for(int j = i+1 ; j < keys.size(); j++){
				if(matches.get(keys.get(j)) > matches.get(keys.get(i))){
					String tkey = keys.get(i);
					keys.set(i, keys.get(j));
					keys.set(j, tkey);
				}
			}
		}
		return keys;
		
	}

}
