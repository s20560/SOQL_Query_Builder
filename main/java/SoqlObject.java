import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoqlObject {

    String objectName;
    int level;
    Map<String, String> fieldsByType = new HashMap<>();
    Map<String, String> relationByType = new HashMap<>();
    List<String> addedFields = new ArrayList<>();

}
