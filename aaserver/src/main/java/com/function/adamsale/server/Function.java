package com.function.adamsale.server;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.*;

//import javax.lang.model.util.ElementScanner14;

import java.io.*;
import java.net.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     *
     */
    private static final String ERROR_OCCURRED = "Error occurred";
    Server ob=new Server();
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("request")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req", 
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("func");
        final String func = request.getBody().orElse(query);
        
        if(func==null)
        {
            return request.createResponseBuilder(HttpStatus.OK).body(ERROR_OCCURRED).build();
        }
        else
        {
            final String lat1 = request.getQueryParameters().get("lat");
            final String lats = request.getBody().orElse(lat1);
            final String lon1 = request.getQueryParameters().get("lon");
            final String lons = request.getBody().orElse(lon1);
            if (func.contains("append")) 
            {
                String resp="";
                try
                {
                    resp=ob.parseRequest("appendDB", lats, lons);
                }
                catch(Exception excep){
                    excep.printStackTrace();
                }
                System.out.println(resp);
                return request.createResponseBuilder(HttpStatus.OK).body(resp).build();
            }    
            else if(func.contains("get"))
            {
                String resp="";
                try
                {
                    resp=ob.parseRequest("getLoc", lats, lons);
                }
                catch(Exception excep){
                    excep.printStackTrace();
                }
                System.out.println(resp);
                return request.createResponseBuilder(HttpStatus.OK).body(resp).build();
            }
            else
            {
                return request.createResponseBuilder(HttpStatus.OK).body("Invalid request").build();
            }
        }
    }
}
class HttpReq 
{
    void sendPOST(Location loc)throws Exception
    {
        URL url=new URL("https://adamsalecosmosdbupdater.azurewebsites.net/api/CosmosRead?func=append&lat="+loc.lat+"&lon="+loc.lon+"&code=ibMvbSx1J8Rtbh5H7vRBH11cqcamcqKNsQF9ohlCp5GnIm6Y0a8lRA==");
        URLConnection uc=url.openConnection();
        BufferedReader br=new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String text="";
        String ln="";
        while((ln=br.readLine())!=null)
        {
            if(ln.contains("lat"))
                text+="\n"+ln;
            else if(ln.contains("lon"))
                text+="\t"+ln;
        }
        //System.out.println(text);
    }
    
    String sendGET() throws Exception
    {
        URL url=new URL("https://adamsalecosmosdbupdater.azurewebsites.net/api/CosmosRead?func=get&code=[API Key. REDACTED]");
        URLConnection uc=url.openConnection();
        BufferedReader br=new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String text="";
        String ln="";
        while((ln=br.readLine())!=null)
        {
            if(ln.contains("lat"))
                text+="\n"+ln;
            else if(ln.contains("lon"))
                text+="\t"+ln;
        }
        //System.out.println(text);
        return text;
    }
}
class Data
{
    HttpReq hr=new HttpReq();
    ArrayList<Location> parseDb()throws Exception
    {
        ArrayList<Location> loc=new ArrayList<Location>();
        String dat=hr.sendGET();
        System.out.println(dat+"\n\n");

        StringTokenizer st=new StringTokenizer(dat," \\\n\t\":,latong");
        int t=st.countTokens();
        //System.out.println(t);
        for(int i=0;i<t;i+=2)
        {
            double a=Double.parseDouble(st.nextToken());
            double b=Double.parseDouble(st.nextToken());
            loc.add(new Location(a,b));
        }
        //System.out.println(loc);
        return loc;
    }
}
class Location
{
    double lat;
    double lon;
    
    Location(double a,double b)
    {
        lat=a;
        lon=b;
    }
    
    public String toString()
    {
        return lat+","+lon;
    }
    public String toStringRev()
    {
        return lon+","+lat;
    }
}
class Server
{

    String parseRequest(String response,String lats, String lons)throws Exception
    {
        double lat=Double.parseDouble(lats);
        double lon=Double.parseDouble(lons);
        if(response.contains("appendDB"))
        {
            HttpReq db=new HttpReq();
            System.out.println(lat+","+lon);
            db.sendPOST(new Location(lat,lon));
            return "Updated";
        }
        else if(response.contains("getLoc"))
        {
            Location origin=new Location(lat,lon);
            Location dest=new Maps().getNearest(origin,new Data().parseDb());
            String res=dest.toString();
            //System.out.println(res);
            return res;
        }
        return response;
    }
}
class Maps
{
    double getDistance(Location src, Location dest)throws Exception
    {
        final String apiKey="[REDACTED]";
        String lnk="https://dev.virtualearth.net/REST/v1/Routes/DistanceMatrix?origins="+src.toString()+"&destinations="+dest.toString()+"&travelMode=driving&key="+ apiKey;
        //System.out.println(lnk+"\n");
        URL url=new URL(lnk);
        URLConnection uc=url.openConnection();
        BufferedReader br=new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String text="";
        String ln="";
        while((ln=br.readLine())!=null)
        {
            text+=ln;
        }
        //System.out.println(text+"\n");
        int k=text.indexOf("\"travelDuration\":");
        text=text.substring(k,text.indexOf("}",k));
        //System.out.println(text+"\n");
        text=text.substring("\"travelDuration\":".length());
        //System.out.println(text);
        return Double.parseDouble(text);
    }

    Location getNearest(Location orig, ArrayList<Location> dest)throws Exception
    {
        if(dest.size()==0)
        {
            return null;
        }
        double dist=9999;
        Location fn=null;
        for(int i=0;i<dest.size();i++)
        {
            double x=getDistance(orig,dest.get(i));
            if(x<0) continue;
            if(x<dist)
            {
                dist=x;
                fn=dest.get(i);
            }
        }
        return fn;
    }
}