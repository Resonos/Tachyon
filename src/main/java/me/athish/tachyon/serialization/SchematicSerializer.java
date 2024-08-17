package me.athish.tachyon.serialization;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import me.athish.tachyon.Schematic;
import org.bukkit.Material;

import java.io.*;
import java.util.HashMap;

public class SchematicSerializer {
    private static final Kryo kryo = new Kryo();

    static {
        kryo.register(Schematic.class);
        kryo.register(SerializableLocation.class);
        kryo.register(Material.class);
        kryo.register(HashMap.class);
    }

    public static void serialize(Schematic schematic, OutputStream os) throws IOException {
        Output output = new Output(os);
        kryo.writeObject(output, schematic);
        output.close();
    }

    public static Schematic deserialize(InputStream is) throws IOException {
        Input input = new Input(is);
        Schematic schematic = kryo.readObject(input, Schematic.class);
        input.close();
        return schematic;
    }
}