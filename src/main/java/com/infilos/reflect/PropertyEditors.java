package com.infilos.reflect;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Optional;

public final class PropertyEditors {
    private PropertyEditors() {
    }

    public static Optional<PropertyEditor> of(final Class<?> type) {
        final PropertyEditor editor = PropertyEditorManager.findEditor(type);

        if (editor != null) {
            return Optional.of(editor);
        }

        final Class<PropertyEditors> c = PropertyEditors.class;

        try {
            String editorClassName = c.getName().replace("PropertyEditors", type.getSimpleName() + "Editor");
            final Class<?> editorClass = c.getClassLoader().loadClass(editorClassName);

            PropertyEditorManager.registerEditor(type, editorClass);

            return Optional.ofNullable(PropertyEditorManager.findEditor(type));
        } catch (final ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
