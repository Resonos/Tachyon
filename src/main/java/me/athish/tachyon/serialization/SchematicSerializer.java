package me.athish.tachyon.serialization;


import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer;
import me.athish.tachyon.Schematic;
import org.bukkit.Material;

import java.io.*;
import java.util.HashMap;

public class SchematicSerializer {
    private static final Kryo kryo = new Kryo();

    static {
        kryo.register(Schematic.class, new FieldSerializer<>(kryo, Schematic.class));
        kryo.register(SerializableLocation.class, new FieldSerializer<>(kryo, SerializableLocation.class));
        kryo.register(Material.class);
        kryo.register(HashMap.class);
    }

    public static void serialize(Schematic schematic, OutputStream os) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(os);
             Output output = new Output(bos)) {
            kryo.writeObject(output, schematic);
        }
    }

    public static Schematic deserialize(InputStream is) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(is);
             Input input = new Input(bis)) {
            return kryo.readObject(input, Schematic.class);
        }
    }
}