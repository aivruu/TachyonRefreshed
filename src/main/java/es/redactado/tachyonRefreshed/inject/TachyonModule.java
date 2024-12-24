package es.redactado.tachyonRefreshed.inject;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import es.redactado.tachyonRefreshed.api.Schematic;
import es.redactado.tachyonRefreshed.api.SchematicService;
import es.redactado.tachyonRefreshed.core.TachyonSchematic;
import es.redactado.tachyonRefreshed.core.TachyonSchematicService;

public class TachyonModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SchematicService.class).to(TachyonSchematicService.class);
        
        install(new FactoryModuleBuilder()
            .implement(Schematic.class, TachyonSchematic.class)
            .build(SchematicFactory.class));
    }
}
