package com.sensei.search.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.search.SortField;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.facets.DefaultFacetHandlerInitializerParam;
import com.browseengine.bobo.facets.impl.PathFacetHandler;
import com.sensei.search.req.SenseiJSONQuery;
import com.sensei.search.req.SenseiRequest;

public class RequestConverter2 {

	public static String[] getStrings(JSONObject obj,String field){
		  String[] strArray = null;
		  JSONArray array = obj.optJSONArray(field);
		  if (array!=null){
			int count = array.length();
			strArray = new String[count];
			for (int i=0;i<count;++i){
				strArray[i] = array.optString(i);
			}
		  }
		  return strArray;
	  }
	  
	  private static int[] getInts(JSONObject obj,String field,int defaultVal){
		  int[] intArray = null;
		  JSONArray array = obj.optJSONArray(field);
		  if (array!=null){
			int count = array.length();
			intArray = new int[count];
			for (int i=0;i<count;++i){
				intArray[i] = array.optInt(i,defaultVal);
			}
		  }
		  return intArray;
	  }
	  
	  private static Set<Integer> getIntSet(JSONObject obj,String field,int defaultVal){
		  HashSet<Integer> intSet = null;
		  JSONArray array = obj.optJSONArray(field);
		  if (array!=null){
			int count = array.length();
			intSet = new HashSet<Integer>(count);
			for (int i=0;i<count;++i){
				intSet.add(array.optInt(i,defaultVal));
			}
		  }
		  return intSet;
	  }
	  
	  public static String[] getStrings(JSONArray jsonArray) throws Exception{
      if (jsonArray == null)
        return null;
		  int count = jsonArray.length();
		  String[] vals = new String[count];
		  for (int i=0;i<count;++i){
			vals[i] = jsonArray.getString(i);
		  }
		  return vals;
	  }
	  
	public static SenseiRequest fromJSON(JSONObject json) throws Exception{
		SenseiRequest req = new SenseiRequest();
		
	    // query
	    req.setQuery(new SenseiJSONQuery(json));
		
		// paging params

	    int  count = json.optInt("size", 10);
	    int  offset = json.optInt("from", 0);
	    req.setCount(count);
	    req.setOffset(offset);
		
		
	    // group by
	    JSONArray groupBy = json.optJSONArray("groupBy");
	    if(groupBy != null)
	    {
	      for(int i=0; i< groupBy.length(); i++)
	      {
	        JSONObject item = groupBy.optJSONObject(i);
	        if(item != null){
	          String column = item.optString("column", null);
	          int top = item.optInt("top", 1);
	          req.setGroupBy(column);
	          req.setMaxPerGroup(top);
	        }
	      }
	    }
		
		// selections
        JSONArray selections = json.optJSONArray("selections");
        if(selections != null)
        {  
          for(int i=0; i<selections.length(); i++)
          {
            JSONObject selItem = selections.optJSONObject(i);
            if(selItem != null){
              Iterator<String> keyIter = selItem.keys();
              while(keyIter.hasNext()){
                String type = keyIter.next();
                JSONObject jsonSel = selItem.optJSONObject(type);
                if(jsonSel != null){
                    addSelection(type, jsonSel, req);
                }
              }
            }
          }

        }
		 // facets
		  JSONObject facets = json.optJSONObject("facets");
		  if (facets!=null){
			  Iterator<String> keyIter = facets.keys();
			  while (keyIter.hasNext()){
				  String field = keyIter.next();
				  JSONObject facetObj = facets.getJSONObject(field);
				  if (facetObj!=null){
					 FacetSpec facetSpec = new FacetSpec();
					 facetSpec.setMaxCount(facetObj.optInt("max", 10));
					 facetSpec.setMinHitCount(facetObj.optInt("minCount", 1));
					 facetSpec.setExpandSelection(facetObj.optBoolean("expand", false));
					 
					 String orderBy = facetObj.optString("order", "hits");
					 FacetSpec.FacetSortSpec facetOrder = FacetSpec.FacetSortSpec.OrderHitsDesc;
					 if ("val".equals(orderBy)){
						 facetOrder = FacetSpec.FacetSortSpec.OrderValueAsc;
					 }
					 
					 facetSpec.setOrderBy(facetOrder);
					 req.setFacetSpec(field, facetSpec);
				  }
			  }
		  }
		  
		  //facet init;
          JSONObject facetInitParams = json.optJSONObject("facetInit");
          if (facetInitParams != null)
          {
            Iterator<String> keyIter = facetInitParams.keys();
            while (keyIter.hasNext())
            {
              // may have multiple facets;
              String facetName = keyIter.next();
              DefaultFacetHandlerInitializerParam param =
                  new DefaultFacetHandlerInitializerParam();
      
              JSONObject jsonParams = facetInitParams.getJSONObject(facetName);
              if (jsonParams != null && jsonParams.length() > 0)
              {
                Iterator<String> paramIter = jsonParams.keys();
                while (paramIter.hasNext())
                {
                  // each facet may have multiple parameters to be configured;
                  String paramName = paramIter.next();
                  JSONObject jsonParamValues = jsonParams.getJSONObject(paramName);
                  String type = jsonParamValues.optString("type", "string");
                  JSONArray jsonValues = jsonParamValues.optJSONArray("values");
                  if (jsonValues != null)
                  {
                    if (type.equals("int"))
                      param.putIntParam(paramName, convertJSONToIntArray(jsonValues));
                    else if (type.equals("string"))
                      param.putStringParam(paramName, convertJSONToStringArray(jsonValues));
                    else if (type.equals("boolean"))
                      param.putBooleanParam(paramName, convertJSONToBoolArray(jsonValues));
                    else if (type.equals("long"))
                      param.putLongParam(paramName, convertJSONToLongArray(jsonValues));
                    else if (type.equals("bytes"))
                      param.putByteArrayParam(paramName, convertJSONToByteArray(jsonValues));
                    else if (type.equals("double"))
                      param.putDoubleParam(paramName, convertJSONToDoubleArray(jsonValues));
                  }
                }
                req.setFacetHandlerInitializerParam(facetName, param);
              }
      
            }
          }
          
		// sorts
		  
          JSONArray sortArray = json.optJSONArray("sort");
          if (sortArray!=null && sortArray.length()>0){
            ArrayList<SortField> sortFieldList = new ArrayList<SortField>(sortArray.length());
            for (int i=0;i<sortArray.length();++i){
              Object obj = sortArray.opt(i);
              if(obj instanceof JSONObject){
                String field = (String) ((JSONObject)obj).keys().next();
                String order = ((JSONObject)obj).optString(field);
                boolean rev = false;
                if("desc".equals(order))
                  rev = true;
                sortFieldList.add(new SortField(field,SortField.CUSTOM,rev));
                continue;
              }
              else if (obj instanceof String){
                if("_score".equals(obj)){
                  sortFieldList.add(SortField.FIELD_SCORE);
                  continue;
                }
              }
            }
            
            
            if (sortFieldList.size()>0){
              req.setSort(sortFieldList.toArray(new SortField[sortFieldList.size()]));
            }
          }
		
		// other
		  
		boolean fetchStored = json.optBoolean("fetchStored");
		req.setFetchStoredFields(fetchStored);
		  
		String[] termVectors = getStrings(json,"termVectors");
		if (termVectors!=null && termVectors.length>0){
		  req.setTermVectorsToFetch(new HashSet<String>(Arrays.asList(termVectors)));
		}
		  

		req.setPartitions(getIntSet(json,"partitions",0));
		  
		req.setShowExplanation(json.optBoolean("explain",false));
		  
		String routeParam = json.optString("routeParam",null);
		req.setRouteParam(routeParam);
		  
		return req;
	}

  private static void addSelection(String type, JSONObject jsonSel, SenseiRequest req) throws Exception
  {
 // we process "term", "terms", "range", "path", "custom" selection types;
    
    
    if("term".equals(type))
    {
      Iterator<String> iter = jsonSel.keys();
      if(iter.hasNext()){
        String facet = iter.next();
        JSONObject jsonParams = jsonSel.optJSONObject(facet);
        String value = jsonParams.optString("value", null);
        if(facet!= null && value != null)
        {
          BrowseSelection sel = new BrowseSelection(facet);
          String[] vals = new String[1];
          vals[0] = value;
          sel.setValues(vals);
          req.addSelection(sel);
        }
      }
    }
    else if("terms".equals(type))
    {
      Iterator<String> iter = jsonSel.keys();
      if(iter.hasNext()){
        String facet = iter.next();
        JSONObject jsonParams = jsonSel.optJSONObject(facet);
        JSONArray values = jsonParams.optJSONArray("values");
        JSONArray excludes = jsonParams.optJSONArray("excludes");
        String operator = jsonParams.optString("operator", "or");
        if(facet!= null && (values != null || excludes != null))
        {
          BrowseSelection sel = new BrowseSelection(facet);
          ValueOperation op = ValueOperation.ValueOperationOr;
          if("and".equals(operator))
            op = ValueOperation.ValueOperationAnd;
          
          if(values != null && values.length()>0){
            sel.setValues(getStrings(values));  
          }

          if(excludes != null && excludes.length()>0){
            sel.setNotValues(getStrings(excludes));  
          }
          
          sel.setSelectionOperation(op);
          req.addSelection(sel);
        }
      }
    }
    else if("range".equals(type))
    {
      Iterator<String> iter = jsonSel.keys();
      if(iter.hasNext()){
        String facet = iter.next();
        JSONObject jsonParams = jsonSel.optJSONObject(facet);
        
        String upper = jsonParams.optString("to", "*");
        String lower = jsonParams.optString("from", "*");
        String range = "["+ lower + " TO " + upper + "]";
        if(facet!= null )
        {
          BrowseSelection sel = new BrowseSelection(facet);
          String[] vals = new String[1];
          vals[0] = range;
          sel.setValues(vals);
          req.addSelection(sel);
        }
      }
    }
    else if("path".equals(type))
    {
      Iterator<String> iter = jsonSel.keys();
      if(iter.hasNext()){
        String facet = iter.next();
        JSONObject jsonParams = jsonSel.optJSONObject(facet);
        
        String value = jsonParams.optString("value", null);

        if(facet != null && value != null){
          BrowseSelection sel = new BrowseSelection(facet);
          String[] vals = new String[1];
          vals[0] = value;
          sel.setValues(vals);
          
          if(jsonParams.has("strict")){
            boolean strict = jsonParams.optBoolean("strict", false);
            sel.getSelectionProperties().setProperty(PathFacetHandler.SEL_PROP_NAME_STRICT, String.valueOf(strict));
          }
          
          if(jsonParams.has("depth")){
            int depth = jsonParams.optInt("depth", 1);
            sel.getSelectionProperties().setProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH, String.valueOf(depth));
          }
          
          req.addSelection(sel);
        }
      }
    }
    else if("custom".equals(type))
    {
      
    }
  }

  /**
   * @param jsonValues
   * @return
   * @throws JSONException
   */
  private static double[] convertJSONToDoubleArray(JSONArray jsonArray) throws JSONException
  {
    double[] doubleArray = new double[jsonArray.length()];
    if (jsonArray != null && jsonArray.length() > 0)
    {
      for (int i = 0; i < jsonArray.length(); i++)
      {
        doubleArray[i] = jsonArray.getDouble(i);
      }
    }
    return doubleArray;
  }

  /**
   * @param jsonValues
   * @return
   * @throws Exception 
   */
  private static byte[] convertJSONToByteArray(JSONArray jsonArray) throws Exception
  {
    if(jsonArray != null && jsonArray.length() == 1)
    {
      String base64 = jsonArray.getString(0);
      byte[] bytes = Base64.decodeBase64(base64);
      return bytes;
    }
    else
      throw new Exception("too many base64 encoded data in one parameter");
  }

  /**
   * @param jsonValues
   * @return
   * @throws JSONException
   */
  private static long[] convertJSONToLongArray(JSONArray jsonArray) throws JSONException
  {
    long[] longArray = new long[jsonArray.length()];
    if (jsonArray != null && jsonArray.length() > 0)
    {
      for (int i = 0; i < jsonArray.length(); i++)
      {
        longArray[i] = jsonArray.getLong(i);
      }
    }
    return longArray;
  }

  /**
   * @param jsonValues
   * @return
   * @throws JSONException
   */
  private static boolean[] convertJSONToBoolArray(JSONArray jsonArray) throws JSONException
  {
    boolean[] boolArray = new boolean[jsonArray.length()];
    if (jsonArray != null && jsonArray.length() > 0)
    {
      for (int i = 0; i < jsonArray.length(); i++)
      {
        boolArray[i] = jsonArray.getBoolean(i);
      }
    }
    return boolArray;
  }

  /**
   * @param jsonValues
   * @return
   * @throws JSONException
   */
  private static List<String> convertJSONToStringArray(JSONArray jsonArray) throws JSONException
  {
    List<String> arString = new ArrayList<String>();
    if (jsonArray != null && jsonArray.length() > 0)
    {
      for (int i = 0; i < jsonArray.length(); i++)
      {
        arString.add(jsonArray.getString(i));
      }
    }
    return arString;
  }

  /**
   * @param jsonValues
   * @return
   * @throws JSONException
   */
  private static int[] convertJSONToIntArray(JSONArray jsonArray) throws JSONException
  {
    int[] intArray = new int[jsonArray.length()];
    if (jsonArray != null && jsonArray.length() > 0)
    {
      for (int i = 0; i < jsonArray.length(); i++)
      {
        intArray[i] = jsonArray.getInt(i);
      }
    }
    return intArray;
  }
}