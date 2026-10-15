# Procedência técnica

## Base reutilizada

Origem: https://github.com/Funny-Bones/ELDEN-RING-Combat-Rewrite

Revisão de referência: af95e33aba93036a6f41d838b226cf25ba7f44f6.

O upstream é um sandbox Rust/Bevy que estuda movimento e combate de Elden Ring. O núcleo em native/src/sim/ adapta principalmente player.rs, data.rs, level.rs e mod.rs. A licença MIT e o crédito de Funny-Bones e colaboradores estão preservados em native/UPSTREAM-LICENSE.md. A adaptação substitui dependências vetoriais de Bevy por glam e acrescenta a ponte C ABI/JNA, comandos para Minecraft, integração física e testes sintéticos. Há mudanças de direção, corrida e seleção de ações para o uso dentro de Minecraft.

Portanto, há código derivado de outro projeto, não apenas inspiração. Elden Flow não é uma alteração do Epic Fight ou Better Combat. Também não é correto descrevê-lo como uma implementação integralmente original da lógica de Elden Ring.

## Pipeline das animações

1. Os arquivos de uma instalação local de Elden Ring são desempacotados. O processo trabalha com arquivos e não precisa executar o jogo.
2. As ferramentas extract.py e bake_anims.py do upstream leem tabelas e animações. O README do upstream descreve UXM Selective Unpack e WitchyBND para preparar os arquivos; essas ferramentas são projetos separados, não autoria do Elden Flow.
3. O adaptador tools/bake-steve.py seleciona ações e articulações relevantes para o Steve, usando o código do upstream. A prévia local usa 965 clips, incluindo movimentos e variações de ataques selecionados.
4. Tabelas geradas alimentam a simulação nativa; movement.bin alimenta a reprodução e adaptação ao corpo segmentado no cliente.
5. Minecraft continua responsável por mundo, itens e entidades. A camada do mod converte os comandos, valida contatos, aplica dano/estamina e sincroniza os jogadores.

Não foram recriadas manualmente todas essas animações. Os dados de movimento usados na prévia são derivados do jogo; a licença do código MIT não transfere direitos sobre os recursos do jogo. O pacote público de código exclui movement.bin, player_anims.bin e extracted.rs.

## Desenvolvido para este projeto

- Integração Java/Fabric e ponte com a simulação Rust.
- Renderização articulada do Steve, encaixe dos itens e integração de armaduras.
- Editor 3D, classificação de armas e presets por mundo.
- Controle de câmera e entradas de teclado/Controlify adaptados ao Minecraft.
- Combate autoritativo, protocolo multiplayer, persistência e permissões.
- Almas, atributos, fogueiras, passivas e interfaces.

Esses sistemas integram uma base adaptada; não tornam o núcleo upstream de autoria deste projeto. A aparência, balanceamento e limitações diferem do Elden Ring original.

## Outras bases

O projeto Java foi iniciado a partir de https://github.com/FabricMC/fabric-example-mod (CC0). Fabric/Fabric API, JNA, glam, Controlify e Mod Menu mantêm suas próprias licenças. Simply Swords fornece itens quando instalado; VulkanMod é um renderer opcional externo. Nenhum deles deve ser apresentado como trabalho nosso.
