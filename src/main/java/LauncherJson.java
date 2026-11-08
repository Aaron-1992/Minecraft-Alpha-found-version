import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/** Small strict JSON codec for local launcher metadata; preserves unknown properties. */
final class LauncherJson {
    private final String input;
    private int position;
    private LauncherJson(String input){this.input=input;}
    static Map<String,Object> object(String input)throws IOException{
        LauncherJson parser=new LauncherJson(input);Object value=parser.value(0);parser.space();
        if(parser.position!=input.length()||!(value instanceof Map))throw new IOException("Expected a JSON object");
        @SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)value;return result;
    }
    private void space(){while(position<input.length()&&" \t\r\n".indexOf(input.charAt(position))>=0)position++;}
    private Object value(int depth)throws IOException{
        if(depth>64)throw fail();space();if(position>=input.length())throw fail();char c=input.charAt(position);
        if(c=='"')return string();
        if(c=='{'){
            position++;Map<String,Object> result=new LinkedHashMap<>();space();if(take('}'))return result;
            do{space();String key=string();space();if(!take(':')||result.containsKey(key))throw fail();result.put(key,value(depth+1));space();if(take('}'))return result;}while(take(','));throw fail();
        }
        if(c=='['){
            position++;List<Object> result=new ArrayList<>();space();if(take(']'))return result;
            do{result.add(value(depth+1));space();if(take(']'))return result;}while(take(','));throw fail();
        }
        for(String literal:List.of("true","false","null"))if(input.startsWith(literal,position)){
            position+=literal.length();return literal.equals("null")?null:Boolean.valueOf(literal);
        }
        int start=position;
        if(take('-')&&position==input.length())throw fail();
        if(!take('0')){if(position>=input.length()||input.charAt(position)<'1'||input.charAt(position)>'9')throw fail();while(digit())position++;}
        if(take('.')){if(!digit())throw fail();while(digit())position++;}
        if(take('e')||take('E')){if(!take('+'))take('-');if(!digit())throw fail();while(digit())position++;}
        try{return new BigDecimal(input.substring(start,position));}catch(NumberFormatException e){throw fail();}
    }
    private boolean digit(){return position<input.length()&&input.charAt(position)>='0'&&input.charAt(position)<='9';}
    private boolean take(char c){if(position<input.length()&&input.charAt(position)==c){position++;return true;}return false;}
    private String string()throws IOException{
        if(!take('"'))throw fail();StringBuilder s=new StringBuilder();
        while(position<input.length()){
            char c=input.charAt(position++);if(c=='"')return s.toString();if(c<32)throw fail();
            if(c=='\\'){
                if(position>=input.length())throw fail();char escape=input.charAt(position++);
                switch(escape){
                    case '"','\\','/' -> s.append(escape);
                    case 'b' -> s.append('\b');case 'f' -> s.append('\f');case 'n' -> s.append('\n');case 'r' -> s.append('\r');case 't' -> s.append('\t');
                    case 'u' -> {if(position+4>input.length())throw fail();try{s.append((char)Integer.parseInt(input.substring(position,position+4),16));}catch(NumberFormatException e){throw fail();}position+=4;}
                    default -> throw fail();
                }
            }else s.append(c);
        }throw fail();
    }
    private IOException fail(){return new IOException("Invalid launcher JSON near character "+position+"; existing file was not changed");}
    static String write(Object value){StringBuilder out=new StringBuilder();append(out,value);return out+"\n";}
    private static void append(StringBuilder out,Object value){
        if(value==null){out.append("null");return;}
        if(value instanceof String s){
            out.append('"');for(char c:s.toCharArray())switch(c){
                case '"' -> out.append("\\\"");case '\\' -> out.append("\\\\");case '\n' -> out.append("\\n");case '\r' -> out.append("\\r");case '\t' -> out.append("\\t");
                default -> {if(c<32)out.append(String.format("\\u%04x",(int)c));else out.append(c);}
            }out.append('"');
        }else if(value instanceof Map<?,?> map){
            out.append('{');boolean first=true;for(var entry:map.entrySet()){if(!first)out.append(',');first=false;append(out,entry.getKey());out.append(':');append(out,entry.getValue());}out.append('}');
        }else if(value instanceof List<?> list){out.append('[');for(int i=0;i<list.size();i++){if(i>0)out.append(',');append(out,list.get(i));}out.append(']');}
        else if(value instanceof Boolean||value instanceof BigDecimal||value instanceof Integer||value instanceof Long)out.append(value);
        else throw new IllegalArgumentException("Unsupported JSON value");
    }
}
