package com.github.Reddishye.tachyonRefreshed.inject;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.github.Reddishye.tachyonRefreshed.api.Schematic;
import com.github.Reddishye.tachyonRefreshed.api.SchematicFactory;
import com.github.Reddishye.tachyonRefreshed.api.SchematicService;
import com.github.Reddishye.tachyonRefreshed.core.TachyonSchematic;
import com.github.Reddishye.tachyonRefreshed.core.TachyonSchematicService;

public class TachyonModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SchematicService.class).to(TachyonSchematicService.class);

        install(new FactoryModuleBuilder()
                .implement(Schematic.class, TachyonSchematic.class)
                .build(SchematicFactory.Create.class));
    }
}
