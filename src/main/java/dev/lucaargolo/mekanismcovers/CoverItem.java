package dev.lucaargolo.mekanismcovers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.lucaargolo.mekanismcovers.mixed.TileEntityTransmitterMixed;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.FluidLogType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CoverItem extends Item {

    public CoverItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext pContext) {
        BlockPos pos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        ItemStack stack = pContext.getItemInHand();
        Block coverBlock = getBlock(stack);
        if(coverBlock != null && entity instanceof TileEntityTransmitterMixed transmitter) {
            BlockState coverState = coverBlock.getStateForPlacement(new BlockPlaceContext(pContext));
            if(!level.isClientSide) {
                if (transmitter.mekanism_covers$getCoverState() != null) {
                    MekanismCovers.removeCover(level, entity, state, pos, transmitter);
                }
                transmitter.mekanism_covers$setCoverState(coverState);
                entity.setChanged();
                stack.shrink(1);
                if (!state.getFluidState().isEmpty()) {
                    level.setBlockAndUpdate(pos, state.setValue(BlockStateHelper.FLUID_LOGGED, FluidLogType.EMPTY));
                }
                level.sendBlockUpdated(pos, state, state, 3);
                level.getLightEngine().checkBlock(pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        if(pLevel != null) {
            Block coverBlock = getBlock(pStack);
            if(coverBlock == null) {
                pTooltipComponents.add(Component.translatable("text.mekanismcovers.empty").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }else{
                pTooltipComponents.add(coverBlock.getName().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            }

        }
    }

    @Nullable
    public static Block getBlock(ItemStack stack) {
        if(!stack.hasTag() || !Objects.requireNonNull(stack.getTag()).contains("CoverBlockItem")) {
            return null;
        }else{
            ItemStack coverBlockItem = ItemStack.of(stack.getOrCreateTag().getCompound("CoverBlockItem"));
            if(coverBlockItem.getItem() instanceof BlockItem blockItem) {
                return blockItem.getBlock();
            }else{
                return null;
            }
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {


            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {

                    @Override
                    public void renderByItem(@NotNull ItemStack pStack, @NotNull ItemDisplayContext pDisplayContext, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
                        Block coverBlock = CoverItem.getBlock(pStack);
                        boolean transparent = true;
                        BakedModel coverStateModel;
                        BlockState coverState;
                        if(coverBlock == null) {
                            coverState = Blocks.AIR.defaultBlockState();
                            coverStateModel = minecraft.getModelManager().getModel(MekanismCovers.COVER_MODEL);
                            transparent = false;
                        }else{
                            coverState = coverBlock.defaultBlockState();
                            coverStateModel = minecraft.getBlockRenderer().getBlockModel(coverState);
                        }
                        BlockState state = Blocks.AIR.defaultBlockState();
                        BakedModel coverModel = minecraft.getModelManager().getModel(MekanismCovers.COVER_MODEL);

                        RenderType renderType = transparent ? RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS) : RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS);
                        VertexConsumer consumer = pBuffer.getBuffer(renderType);

                        PoseStack.Pose pose = pPoseStack.last();
                        RandomSource random = RandomSource.create();

                        for(Direction direction : Direction.values()) {
                            random.setSeed(42L);
                            renderQuadList(coverState, pose, consumer, coverStateModel.getQuads(coverState, direction, random, ModelData.EMPTY, renderType), pPackedLight, pPackedOverlay);
                        }

                        random.setSeed(42L);
                        renderQuadList(coverState, pose, consumer, coverStateModel.getQuads(coverState, null, random, ModelData.EMPTY, renderType), pPackedLight, pPackedOverlay);

                        if(coverStateModel != coverModel) {
                            for(Direction direction : Direction.values()) {
                                random.setSeed(42L);
                                renderQuadList(state, pose, consumer, coverModel.getQuads(state, direction, random, ModelData.EMPTY, renderType), pPackedLight, pPackedOverlay);
                            }

                            random.setSeed(42L);
                            renderQuadList(state, pose, consumer, coverModel.getQuads(state, null, random, ModelData.EMPTY, renderType), pPackedLight, pPackedOverlay);
                        }
                    }

                    private void renderQuadList(BlockState state, PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> bakedQuads, int light, int overlay) {
                        for(BakedQuad quad : bakedQuads) {
                            float f;
                            float f1;
                            float f2;
                            if (minecraft.level != null && quad.isTinted()) {
                                BlockPos pos = BlockPos.ZERO;
                                if(minecraft.player != null) {
                                    pos = minecraft.player.blockPosition();
                                }
                                int i = minecraft.getBlockColors().getColor(state, minecraft.level, pos, quad.getTintIndex());
                                f = (float)(i >> 16 & 255) / 255.0F;
                                f1 = (float)(i >> 8 & 255) / 255.0F;
                                f2 = (float)(i & 255) / 255.0F;
                            } else {
                                f = 1.0F;
                                f1 = 1.0F;
                                f2 = 1.0F;
                            }

                            consumer.putBulkData(pose, quad, f, f1, f2, light, overlay);
                        }
                    }
                };

            }


        });
    }
}
