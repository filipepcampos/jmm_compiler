package pt.up.fe.comp.analysis;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class JmmType extends Type {
    boolean anyType = false;

    public JmmType(String name, boolean isArray, boolean anyType){
        super(name, isArray);
        this.anyType = anyType;
    }

    public JmmType(String name, boolean isArray){
        super(name, isArray);
        this.anyType = false;
    }

    public JmmType(Type type){
        super(type.getName(), type.isArray());
        this.anyType = false;
    }

    public boolean equals(Type other){
        if (this.anyType)
            return true;
        if (this.isArray() != other.isArray())
            return false;
        if (this.getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!this.getName().equals(other.getName()))
            return false;
        return true;
    }

    public boolean equals(Object obj){
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() == obj.getClass()){
            JmmType other = (JmmType) obj;
            if(this.anyType == true || other.anyType == true){
                return true;
            }
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        if(this.anyType){
            return "Type [ANY]";
        }
        return super.toString();
    }
}
