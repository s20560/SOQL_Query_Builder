import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import java.io.IOException;

public class BuilderWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        BuilderWindow bw = null;
        try {
            bw = new BuilderWindow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(bw.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
