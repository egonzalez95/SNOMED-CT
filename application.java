/*
Author: Emilio Gonzalez
Intended Use: To analyze SNOMED-CT's knowledgebase for inconsistencies
              using symmetric modifier pairs. (Using SnAPI)
Progress: Running, need to add method to compare sibling parent
Scripted for NYC CUNY/BMCC LSAMP Research Program.
Mentor: Yan Chen
Started Spring 2015. Finished October 24, 2015
*/
package snomedct;

import java.util.*;
import java.io.*;

public class application 
{
    public static final String GUID="cf78ed81-84fb-472d-b878-7f42756473de";
    public static final String namespace="0";
    public static final int reltype=116680003;
    public static int concept_count=0;
    public static int consistent_count=0;
    public static int error_count=0;
    public static int modifier_count=0;
    public static long concept_ID;
    public static long starting_concept_ID;
    public static final String[] modList={"upper","lower","acquired","congenital",
                                "primary","secondary","superior","inferior",
                                "anterior","posterior","left","right ",
                                "lateral","unilateral","ascending","descending",
                                "acute","chronic","first","second"};
    
    public static void main(String[] args)
    {
        try
        {
            snomedct.SnApiService service = new snomedct.SnApiService();//snapi service
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();//snapi service
            Scanner usr_input=new Scanner(System.in);
            System.out.println("Insert starting concept: [0 is predefined concept]");
            System.out.print("> ");
            concept_ID=usr_input.nextLong();
            if(concept_ID==0){
                concept_ID=118945008;
                System.out.println("predefined audit has 732 concepts, 89 concepts should be inconsistent.");
            }
            starting_concept_ID=concept_ID;
            System.out.println("Processsing audit for starting concept: '"+(concept_ID+1)+"' : "+getConceptFSN(concept_ID));
            identify(concept_ID);
            System.out.println("\nCompilation Results:");
            System.out.println("Concepts Scanned: "+concept_count);
            System.out.println("Concept Consistencies: "+consistent_count);
            System.out.println("Amount of Modifiers found: "+modifier_count);  
            System.out.println("Audit Result: "+(((double)consistent_count/concept_count)*100)+"% consistent");
            System.out.println("Audit Result: "+(((double)error_count/concept_count)*100)+"% inconsistent");
            System.out.println("Starting Concept ID: "+starting_concept_ID);
            System.out.println("Starting Concept FSN: "+getConceptFSN(starting_concept_ID));
            System.out.println("GUID: "+GUID);
            final_log(starting_concept_ID);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    //RUNS ALL SEARCH MODULES
    public static void identify(long active_concept) throws IOException
    {
        try
        {
            concept_count++;
            System.out.println("["+concept_count+"] - "+"SNOMED-ID: "+active_concept+" ("+getConceptFSN(active_concept)+")");
            String FSN=getConceptFSN(active_concept);
            if(containsModPair(FSN)==true)
                    {
                       modifier_count++;
                       System.out.print("\t\tWorking... ");
                       String FSN_pair=getConceptModPair(FSN);
                       String[] values=new String[5];
                       if(searchForFSN(FSN,FSN_pair,"accuracy")[0].equals("true"))
                       {
                           for(int x=0; x<5; x++)
                            {
                                values[x]=searchForFSN(FSN,FSN_pair,"accuracy")[x];
                            }
                           System.out.print("  Mod pair exists. Checking Parents..");
                           long p1=getParent(active_concept);
                           long p2=getParent(Long.parseLong(values[1]));
                           if(p1==p2){
                               System.out.println(" Parents Match.");
                               consistent_count++;}
                           
                           else{
                               if(p2==0)
                               {
                                   System.out.println(">> *** Sys Err 4, Parent doesnt exist - fail");
                                   values[2]="4";
                                   error_count++;
                                   report(FSN,FSN_pair,values[2],Double.parseDouble(values[4]));
                               }
                               else
                               {
                                   System.out.println(">> *** Sys Err 2, Parent Mismatch");
                                   values[2]="2";
                                   error_count++;
                                   report(FSN,FSN_pair,values[2],Double.parseDouble(values[4]));
                               }
                           }
                       }   
                    }
            else{
            //System.out.println("OK, keep going");
            consistent_count++;
            getChildren(active_concept);}
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    
    // GETS A CONCEPT FSN
    public static String getConceptFSN(long active_concept_ID)
    {
        try
        {
            snomedct.SnApiService service = new snomedct.SnApiService();//snapi service
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();//snapi service
            ArrayOfDescription as = port.getDescription(active_concept_ID, GUID);
            String concept_name=as.description.get(0).term.getValue().toLowerCase(); // save to string
            return concept_name;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            return "Error - getConceptFSN(long) method.";
        }
    }
    
    
    public static boolean containsModPair(String active_concept_FSN)
    {
        for(int modNo=0; modNo<modList.length; modNo++)
            {
                String FSN=active_concept_FSN.toLowerCase();
                if(FSN.contains(modList[modNo]))
                {
                    return true;
                }
            }
        return false;
    }


    //SEARCHES A CONCEPT FOR A MODIFIER PAIR
    public static String getConceptModPair(String active_concept_FSN)
    {
        try
        {
            String newConceptFSN="";
            for(int modNo=0; modNo<modList.length; modNo++)
            { 
                String FSN=active_concept_FSN.toLowerCase();
                if(FSN.contains(modList[modNo]))
                {
                    //--------------- all cases of modifier switching ------------------------------------//
                    if(modList[modNo].equals("upper"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "lower"); //replace modifier with its opposite
                    if(modList[modNo].equals("lower"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "upper"); //replace modifier with its opposite
                    if(modList[modNo].equals("congenital"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "acquired");//replace modifier with its opposite
                    if(modList[modNo].equals("acquired"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "congenital"); //replace modifier with its opposite
                    if(modList[modNo].equals("primary"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "secondary"); //replace modifier with its opposite
                    if(modList[modNo].equals("secondary"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "primary"); //replace modifier with its opposite
                    if(modList[modNo].equals("superior"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "inferior");//replace modifier with its opposite
                    if(modList[modNo].equals("inferior"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "superior"); //replace modifier with its opposite
                    if(modList[modNo].equals("anterior"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "posterior"); //replace modifier with its opposite
                    if(modList[modNo].equals("posterior"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "anterior"); //replace modifier with its opposite
                    if(modList[modNo].equals("left"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "right");//replace modifier with its opposite
                    if(modList[modNo].equals("right"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "left"); //replace modifier with its opposite
                    if(modList[modNo].equals("lateral"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "unilateral");//replace modifier with its opposite
                    if(modList[modNo].equals("unilateral"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "lateral"); //replace modifier with its opposite
                    if(modList[modNo].equals("ascendng"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "descencding"); //replace modifier with its opposite
                    if(modList[modNo].equals("descencding"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "ascending"); //replace modifier with its opposite
                    if(modList[modNo].equals("acute"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "chronic");//replace modifier with its opposite
                    if(modList[modNo].equals("chronic"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "acute"); //replace modifier with its opposite
                    if(modList[modNo].equals("first"))
                        newConceptFSN=FSN.replaceAll(modList[modNo], "second"); //replace modifier with its opposite
                    //--------------- all cases of modifier switching - done -----------------------------//
                //System.out.println("*"+newConceptFSN);                             
                }
            }
        return newConceptFSN; 
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            return "Error - getConceptModPair(String) method.";
        }
    }
    
    
    // SEARCHES FOR A CONCEPT VIA FSN, I CHOSE THIS METHOD TO DO MORE THAN ONE TASK PURPOSELY STRING[]
    public static String[] searchForFSN(String original_concept, String active_concept, String search_mode)
    {
        String[] search_results=new String[5];
        try
        {
            String error_code="";
            boolean isConcept=false;
            String result;
            double result_accuracy;
            snomedct.SnApiService service = new snomedct.SnApiService(); //snapi service
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService(); //snapi service
            ArrayOfConcept as = port.searchConcepts( 
                active_concept,    //search string
                "",                //descid
                "",                //status
                "",                //fav
                "",                //ancestor
                "",                //target
                "",                //mapset
                "",                //subsetid
                "",                //favourite
                "",                //suffix
                namespace,         //namespace
                1,                 //max results (we only want 1, but I will get top 3 results for matching)
                0,                 //match type (lexical)(whole word matching)
                "S",               //srchmde (sentence)
                GUID);             //instance of ArrayOfConcept
                //predifined all search criteria so there are no problems with ArrayOfConcept.
            result=as.concept.get(0).fullySpecifiedName.getValue().toLowerCase();
            result_accuracy=as.concept.get(0).weight.getValue().doubleValue();
            //System.out.print("\t\t"+result);
            //System.out.println("\t"+result_accuracy);
            //searchFSNParent();
            if(search_mode.equals("accuracy")){
                    if(result_accuracy==100.00)
                        {
                            isConcept=true;
                            //System.out.println(">> Concept Found");
                        }
                    else if(result_accuracy>=85.00 && result_accuracy!=100.00) //threshold is 85%
                        {
                            isConcept=false;
                            System.out.println(">> *** Sys Err 3, Possible false-non match or typo."+result_accuracy);
                            //report_error(original_concept,active_concept,result,result_accuracy);
                            error_code="3";
                            error_count++;
                            report(original_concept,active_concept,error_code,result_accuracy);

                        }
                    else
                        {
                            isConcept=false;
                            System.out.println(">> *** Sys Err 1, Concept not found. Match Rate: "+result_accuracy);
                            //report_error(original_concept,active_concept,result,result_accuracy);
                            error_code="1";
                            error_count++;
                            report(original_concept,active_concept,error_code,result_accuracy);
                        }
            }
            search_results[0]=String.valueOf(isConcept);
            search_results[1]=String.valueOf(as.concept.get(0).conceptId);
            search_results[2]=error_code;
            search_results[3]=result;
            search_results[4]=String.valueOf(result_accuracy);
            return search_results;

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            return search_results;
        }
    }
    
    
    // SEARCHES FOR CHILDREN OF ACTIVE CONCEPT
    public static void getChildren(long target_concept_ID)
    {
        try
        {
            snomedct.SnApiService service = new snomedct.SnApiService();//snapi service
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();//snapi service
            ArrayOfNeighbour as = port.getNeighbours(target_concept_ID, namespace, reltype, GUID);
            //System.out.println(getConceptFSN(target_concept_ID));
            for (int i = 0; i < as.neighbour.size(); i++)
            {
                String PSC=as.neighbour.get(i).porCHorS.getValue(); //PSC= Parent, Self or Child
                if (PSC.equals("C")) // "C" represents Child
                {   
                    long child_ID=as.neighbour.get(i).neighbourConceptId; 
                    identify(child_ID); //recursive
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error getChildren(long)");
        }
    }
    
    
        // SEARCHES FOR CHILDREN OF ACTIVE CONCEPT
    public static long getParent(long target_concept_ID)
    {
        try
        {
            snomedct.SnApiService service = new snomedct.SnApiService();//snapi service
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();//snapi service
            ArrayOfNeighbour as = port.getNeighbours(target_concept_ID, namespace, reltype, GUID);
            //System.out.println(getConceptFSN(target_concept_ID));
            for (int i = 0; i < as.neighbour.size(); i++)
            {
                String PSC=as.neighbour.get(i).porCHorS.getValue(); //PSC= Parent, Self or Child
                if (PSC.equals("P")) // "C" represents Child
                {   
                   return as.neighbour.get(i).neighbourConceptId;
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Error getParent(String)");
        }
        return 0;
    }   
    
    
    //ERROR LOGGER
    private static void report(String FSN, String FSN_M, String error_code, double accuracy) throws IOException
    {
        //where FSN is originl and FSN_M is modifier pair, the 2 lines under this code get concepts ID.
        long FSN_ID=Long.parseLong(searchForFSN(FSN,FSN,"")[1]);
        long FSN_M_ID=Long.parseLong(searchForFSN(FSN_M,FSN_M,"")[1]);
        PrintWriter error_log=new PrintWriter(new FileWriter("/Users/emiliogonzalez/Desktop/error_log.txt",true));
        if(error_code.equals("1"))
        {
            error_log.println("====================");
            error_log.println("Error #"+error_count+"  (SNOMED ID: "+FSN_ID+" - "+FSN);
            error_log.println("> Modifier pair FSN '"+FSN_M+"' \n    for '"+FSN+"' does not exist.");
            error_log.println("> First result: "+searchForFSN(FSN,FSN_M,"")[3]);
            error_log.println("> First result yielded a "+accuracy+"% match.");
            error_log.println("> ERROR ID: 1 (MOD_PAIR_NOT_EXIST)");
            error_log.println("Position in audit: "+concept_count);
            error_log.println("====================");
            error_log.println("");
        }
        else if(error_code.equals("2"))
        {
            error_log.println("====================");
            error_log.println("Error #"+error_count+"  (SNOMED ID: "+FSN_ID+" - "+FSN);
            error_log.println("> Modifier pair '"+FSN_M+"' was found \n    for '"+FSN+"'");
            error_log.println("> Both children yielded different parents though.");
            error_log.println("> Parent for "+FSN+":");
            error_log.println("    "+getConceptFSN(getParent(FSN_ID)));
            error_log.println("> Parent for "+FSN_M+":");
            error_log.println("    "+getConceptFSN(getParent(FSN_M_ID)));
            error_log.println("> ERROR ID: 2 (PARENT_MISMATCH)");
            error_log.println("Position in audit: "+concept_count);
            error_log.println("====================");
            error_log.println("");
        }
        else if(error_code.equals("3"))
        {
            error_log.println("====================");
            error_log.println("Error #"+error_count+"  (SNOMED ID: "+FSN_ID+" - "+FSN);
            error_log.println("> Modifier pair FSN '"+FSN_M+"' \n    for '"+FSN+"' does not exist or might be mispelled.");
            error_log.println("> First result: "+searchForFSN(FSN,FSN_M,"")[3]);
            error_log.println("  First result yielded a "+accuracy+"% match.");
            error_log.println("> ERROR ID: 3 (MOD_PAIR_NO_EXIST_OR_MISTYPED)");
            error_log.println("Position in audit: "+concept_count);
            error_log.println("====================");
            error_log.println("");
        }
        else if(error_code.equals("4"))
        {
            error_log.println("====================");
            error_log.println("Error #"+error_count+"  (SNOMED ID: "+FSN_ID+" - "+FSN);
            error_log.println("> Modifier pair for '"+FSN+"' was found \n    for '"+FSN+"'");
            error_log.println("> One child had a parent, the other did not.");
            
            if(getParent(FSN_ID)!=0){
            error_log.println("> Parent for "+FSN+":");
            error_log.println("    "+getConceptFSN(getParent(FSN_ID)));}
            else{
                error_log.println("> Parent for "+FSN+":");
                error_log.println("    NO PARENT EXISTS");
            }
            
            if(getParent(FSN_M_ID)!=0){
                error_log.println("> Parent for "+FSN_M+":");
                error_log.println("    "+getConceptFSN(getParent(FSN_M_ID)));}
            else{
                error_log.println("> Parent for "+FSN_M+":");
                error_log.println("    NO PARENT EXISTS");
            }
            
            error_log.println("> ERROR ID: 4 (PARENT_NO_EXIST)");
            error_log.println("Position in audit: "+concept_count);
            error_log.println("====================");
            error_log.println("");
        }
        else
        {
            error_log.println("====================");
            error_log.println("Error #"+error_count+"  (SNOMED ID: "+FSN_ID+" - "+FSN);
            error_log.println("> No information is available for this error.");
            error_log.println("> ERROR ID: 0 (NO_ERR_INFO)");
            error_log.println("Position in audit: "+concept_count);
            error_log.println("====================");
            error_log.println("");
        }
        error_log.close();
    }
        
    private static void final_log(long start_id) throws IOException
        {
            PrintWriter logger = new PrintWriter("/Users/emiliogonzalez/Desktop/final-log.txt");
            logger.println("Compilation Results:");
            logger.println("Concepts Scanned: "+concept_count);
            logger.println("Concept Consistencies: "+(consistent_count));
            logger.println("Concept Inconsistencies: "+error_count);
            logger.println("Amount of Modifiers found: "+modifier_count);
            logger.println("Audit Result: "+(((double)consistent_count/concept_count)*100)+"% consistent");
            logger.println("Audit Result: "+(((double)error_count/concept_count)*100)+"% inconsistent");
            logger.println("Starting Concept ID: "+start_id);
            logger.println("Starting Concept FSN: "+getConceptFSN(start_id));
            logger.close();
        }    
        
        
        
    //SnAPI - SNOMED CORE COMPONENTS
    private static ArrayOfConcept getConcept(java.lang.String conceptId, java.lang.String nameSpace, java.lang.String guid) 
        {
            snomedct.SnApiService service = new snomedct.SnApiService();
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();
            return port.getConcept(conceptId, nameSpace, guid);
        }

    private static ArrayOfDescription getDescription(java.lang.Long conceptId, java.lang.String guid) 
        {
            snomedct.SnApiService service = new snomedct.SnApiService();
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();
            return port.getDescription(conceptId, guid);
        }

    private static ArrayOfNeighbour getNeighbours(java.lang.Long conceptId, java.lang.String nameSpace, java.lang.Integer relType, java.lang.String guid) 
        {
            snomedct.SnApiService service = new snomedct.SnApiService();
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();
            return port.getNeighbours(conceptId, nameSpace, relType, guid);
        }

    private static ArrayOfConcept searchConcepts(java.lang.String text, java.lang.String descendentId, java.lang.String status, java.lang.String ancestorId, java.lang.String saveToFavouriteId, java.lang.String targetCodes, java.lang.String mapSetId, java.lang.String subsets, java.lang.String favorites, java.lang.String suffix, java.lang.String namespaces, java.lang.Integer maxResults, java.lang.Integer matchType, java.lang.String searchMode, java.lang.String guid) 
        {
            snomedct.SnApiService service = new snomedct.SnApiService();
            snomedct.ISnApiService port = service.getBasicHttpBindingISnApiService();
            return port.searchConcepts(text, descendentId, status, ancestorId, saveToFavouriteId, targetCodes, mapSetId, subsets, favorites, suffix, namespaces, maxResults, matchType, searchMode, guid);
        } 
}
