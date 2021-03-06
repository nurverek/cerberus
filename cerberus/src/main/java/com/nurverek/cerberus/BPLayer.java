package com.nurverek.cerberus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.opengl.GLES32;

import com.nurverek.firestorm.FSBufferLayout;
import com.nurverek.firestorm.FSBufferManager;
import com.nurverek.firestorm.FSConfig;
import com.nurverek.firestorm.FSControl;
import com.nurverek.firestorm.FSG;
import com.nurverek.firestorm.FSGAssembler;
import com.nurverek.firestorm.FSGScanner;
import com.nurverek.firestorm.FSInstance;
import com.nurverek.firestorm.FSLinkType;
import com.nurverek.firestorm.FSMesh;
import com.nurverek.firestorm.FSP;
import com.nurverek.firestorm.FSTexture;
import com.nurverek.firestorm.FSTools;
import com.nurverek.firestorm.FSVertexBuffer;
import com.nurverek.vanguard.VLArrayFloat;
import com.nurverek.vanguard.VLBufferFloat;
import com.nurverek.vanguard.VLInt;
import com.nurverek.vanguard.VLListType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class BPLayer extends CustomBluePrint{

    public static final int INSTANCE_COUNT = 24;
    private static final int LAYER_PIECE_TEXTURE_DIMENSION = 512;
    public static ByteBuffer PIXEL_BUFFER = null;

    public FSP program;
    public int bindpointoffset;

    public BPLayer(FSG gen){
        initialize(gen);

        bindpointoffset = Gen.UBOBINDPOINT;
        Gen.UBOBINDPOINT++;
    }

    @Override
    protected void createPrograms(){
        program = new FSP(Gen.DEBUG_MODE_PROGRAMS);
        program.modify(new ModModel.UBO(1, com.shayan.shapecity.BPLayer.INSTANCE_COUNT), FSConfig.POLICY_ALWAYS);
        program.modify(new com.shayan.shapecity.ModColor.TextureAndUBO(1, com.shayan.shapecity.BPLayer.INSTANCE_COUNT, true, false, true), FSConfig.POLICY_ALWAYS);
        program.modify(new ModLight.Point(Gen.GAMMA, null, Gen.BRIGHTNESS, Gen.light, null, com.shayan.shapecity.Material.MATERIAL_WHITE_RUBBER.getGLSLSize()), FSConfig.POLICY_ALWAYS);
        program.addMeshConfig(new FSP.DrawElementsInstanced(FSConfig.POLICY_ALWAYS, 0));
        program.build();
    }

    @Override
    protected void attachPrograms(FSG gen){
        gen.programSet(Gen.MAIN_PROGRAMSET).add(program);
    }

    @Override
    public FSGScanner register(FSG gen, String name){
        FSGAssembler config = new FSGAssembler();
        config.SYNC_MODELMATRIX_AND_MODELARRAY = true;
        config.SYNC_MODELARRAY_AND_SCHEMATICS = true;
        config.SYNC_MODELARRAY_AND_BUFFER = true;
        config.SYNC_POSITION_AND_SCHEMATICS = true;
        config.SYNC_POSITION_AND_BUFFER = true;
        config.SYNC_COLOR_AND_BUFFER = true;
        config.SYNC_TEXCOORD_AND_BUFFER = true;
        config.SYNC_NORMAL_AND_BUFFER = true;
        config.SYNC_INDICES_AND_BUFFER = true;
        config.INSTANCE_SHARE_POSITIONS = true;
        config.INSTANCE_SHARE_COLORS = false;
        config.INSTANCE_SHARE_TEXCOORDS = true;
        config.INSTANCE_SHARE_NORMALS = true;
        config.LOAD_MODELS = true;
        config.LOAD_POSITIONS = true;
        config.LOAD_COLORS = false;
        config.LOAD_TEXCOORDS = true;
        config.LOAD_NORMALS = true;
        config.LOAD_INDICES = true;
        config.CONVERT_POSITIONS_TO_MODELARRAYS = true;
        config.DRAW_MODE_INDEXED = true;
        config.configure();

        return new FSGScanner.Instanced(this, config, name, GLES32.GL_TRIANGLES, INSTANCE_COUNT);
    }

    @Override
    protected void preAssemblyAdjustment(FSMesh mesh, FSInstance instance){
        super.preAssemblyAdjustment(mesh, instance);
    }

    @Override
    protected void postScanAdjustment(FSMesh mesh){
        FSTexture texture = new FSTexture(new VLInt(GLES32.GL_TEXTURE_2D_ARRAY), new VLInt(Gen.TEXUNIT++));
        texture.bind();
        texture.storage3D(1, GLES32.GL_RGBA8, LAYER_PIECE_TEXTURE_DIMENSION, LAYER_PIECE_TEXTURE_DIMENSION, com.shayan.shapecity.BPLayer.INSTANCE_COUNT);
        texture.minFilter(GLES32.GL_LINEAR);
        texture.magFilter(GLES32.GL_LINEAR);
        texture.wrapS(GLES32.GL_CLAMP_TO_EDGE);
        texture.wrapT(GLES32.GL_CLAMP_TO_EDGE);
        texture.baseLevel(0);
        texture.maxLevel(com.shayan.shapecity.BPLayer.INSTANCE_COUNT - 1);
        texture.unbind();

        int size = mesh.size();

        for(int i = 0; i < size; i++){
            mesh.instance(i).colorTexture(texture);
        }
    }

    @Override
    public void createLinks(FSMesh mesh){
        float[] array = new float[INSTANCE_COUNT];
        Arrays.fill(array, Puzzle.TEXCONTROL_IDLE);

        VLListType<FSLinkType> links = new VLListType<>(1, 0);
        links.add(new com.shayan.shapecity.ModColor.TextureControlLink(new VLArrayFloat(array)));

        mesh.initLinks(links);
    }

    @Override
    public FSBufferLayout bufferLayouts(FSMesh mesh, FSBufferManager manager){
        int modelbuffer = manager.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, bindpointoffset), new VLBufferFloat()));
        int texcontrolbuffer = manager.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, bindpointoffset + 1), new VLBufferFloat()));
        int colorbuffer = manager.add(new FSBufferManager.EntryFloat(new FSVertexBuffer(GLES32.GL_UNIFORM_BUFFER, GLES32.GL_DYNAMIC_DRAW, bindpointoffset + 2), new VLBufferFloat()));

        FSBufferLayout layout = new FSBufferLayout(mesh);
        layout.add(manager, modelbuffer, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INSTANCED, FSG.ELEMENT_MODEL));

        layout.add(manager, colorbuffer, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INSTANCED, FSG.ELEMENT_COLOR));

        layout.add(manager, Gen.BUFFER_ARRAY_FLOAT_DEFAULT, 3)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, FSG.ELEMENT_POSITION))
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, FSG.ELEMENT_TEXCOORD))
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_INTERLEAVED_SINGULAR, FSG.ELEMENT_NORMAL));

        layout.add(manager, Gen.BUFFER_ELEMENT_SHORT_DEFAULT, 1)
                .addElement(new FSBufferLayout.EntryElement(FSBufferLayout.ELEMENT_SEQUENTIAL_INDICES, FSG.ELEMENT_INDEX));

        layout.add(manager, texcontrolbuffer, 1)
                .addLink(new FSBufferLayout.EntryLink(FSBufferLayout.LINK_SEQUENTIAL_SINGULAR, 0, 0, 1, 1, 4));

        return layout;
    }

    @Override
    protected void postBufferAdjustment(FSMesh mesh){

    }

    @Override
    protected void attachMeshToPrograms(FSMesh mesh){
        program.addMesh(mesh);
    }

    @Override
    protected void finished(FSMesh mesh){

    }

    public int[] prepareMatchSymTexture(FSMesh mesh){
        Bitmap b = null;

        FSTexture texture = mesh.instance(0).colorTexture();

        int[] resources = new int[]{ R.drawable.circle, R.drawable.hex, R.drawable.square, R.drawable.triangle, R.drawable.rsquare };
        int[] timespicked = new int[resources.length];

        Arrays.fill(timespicked, 0);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);

        Context cxt = FSControl.getContext();

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.outConfig = Bitmap.Config.ARGB_8888;
        opts.inScaled = true;
        opts.inMutable = true;

        int choice = 0;
        int requiredchoices = com.shayan.shapecity.BPLayer.INSTANCE_COUNT / Game.GAME_MATCHSYM_PICK_LIMIT;
        int index = 0;

        PIXEL_BUFFER = null;
        texture.bind();

        int[] symbols = new int[com.shayan.shapecity.BPLayer.INSTANCE_COUNT];
        Arrays.fill(symbols, -1);

        for(int i = 0; i < requiredchoices; i++){
            choice = Gen.RANDOM.nextInt(resources.length);

            while(timespicked[choice] >= Game.GAME_MATCHSYM_REPEAT_ICON_LIMIT){
                choice = Gen.RANDOM.nextInt(resources.length);
            }

            timespicked[choice]++;

            b = BitmapFactory.decodeResource(cxt.getResources(), resources[choice], opts);

            if(PIXEL_BUFFER == null){
                PIXEL_BUFFER = ByteBuffer.allocate(b.getAllocationByteCount());
                PIXEL_BUFFER.order(ByteOrder.nativeOrder());
            }

            PIXEL_BUFFER.position(0);

            b.copyPixelsToBuffer(PIXEL_BUFFER);
            b.recycle();

            for(int i2 = 0; i2 < Game.GAME_MATCHSYM_PICK_LIMIT; i2++){
                index = Gen.RANDOM.nextInt(com.shayan.shapecity.BPLayer.INSTANCE_COUNT);

                while(symbols[index] != -1){
                    index = Gen.RANDOM.nextInt(com.shayan.shapecity.BPLayer.INSTANCE_COUNT);
                }

                symbols[index] = choice;

                PIXEL_BUFFER.position(0);
                texture.subImage3D(0, 0, 0, index, LAYER_PIECE_TEXTURE_DIMENSION, LAYER_PIECE_TEXTURE_DIMENSION, 1, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, PIXEL_BUFFER);
            }
        }

        PIXEL_BUFFER = null;

        FSTools.checkGLError();
        texture.unbind();

        return symbols;
    }
}
