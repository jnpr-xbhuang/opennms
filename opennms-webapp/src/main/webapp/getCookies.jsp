<% Cookie cookies[] = request.getCookies();
String resp = "";
if(cookies!=null){
        for(int i=0; i<cookies.length; i++){
                if("jsessionid".equalsIgnoreCase(cookies[i].getName())){
                            resp += "JSESSIONID="+cookies[i].getValue()+";";
                }
        }
        out.print(resp);
}%>
