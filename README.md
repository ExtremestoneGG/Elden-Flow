# Elden Flow

Movimentação e combate inspirados em Elden Ring para o Steve, integrados ao mundo de Minecraft: armas, esquiva, defesa, lock-on, fogueiras e progressão por almas.

**Em desenvolvimento para Minecraft 1.21.1 / Fabric / Java 21 / Windows x64.** A última prévia de teste é a 0.9.2; a 1.0 ainda não foi declarada pronta. Forge, Linux e macOS não estão disponíveis nesta etapa.

## Origem e créditos

Elden Flow não foi construído inteiramente do zero. Seu núcleo de simulação Rust adapta código do [ELDEN-RING-Combat-Rewrite, de Funny-Bones e colaboradores](https://github.com/Funny-Bones/ELDEN-RING-Combat-Rewrite/tree/af95e33aba93036a6f41d838b226cf25ba7f44f6), um sandbox em Bevy, sob licença MIT. Essa reutilização inclui a máquina de estados e definições de ações, além das ferramentas de extração de dados e animações. Não é apenas uma ferramenta externa usada para ler arquivos.

A integração Fabric, o Steve articulado, o editor de armas, as interfaces e os sistemas de Minecraft foram desenvolvidos neste projeto. O esqueleto inicial de Gradle/Fabric vem do fabric-example-mod (CC0). Não usamos código ou assets de Epic Fight ou Better Combat. O fato de o repositório ser publicado separadamente não elimina a origem do código adaptado.

Veja [procedência técnica e pipeline](docs/PROVENANCE.md) e [avisos de licença](NOTICE.md).

## Recursos implementados

- Movimentação, saltos, esquivas com i-frames, lock-on e transições de câmera.
- Ataques leves, pesados e carregados; variações de empunhadura e pares de armas compatíveis.
- Escudos, defesa, parry, estamina e passivas por classe de arma.
- Identificação de armas, ajustes manuais por item e editor 3D de encaixe.
- Steve segmentado, skins e adaptação de armadura.
- Almas, atributos e descanso/progressão em fogueiras do Minecraft.
- Integração opcional com Controlify e Mod Menu.
- Combate e progressão decididos no servidor; configurações de armas pertencem ao mundo, inclusive na LAN.

São recursos implementados em uma prévia, sujeitos a correções. Compatibilidade automática por nome não garante identificação perfeita de todas as armas de todos os mods.

## Compatibilidade

| Componente | Situação |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | Fabric; metadados exigem >= 0.16.0 |
| Java | 21 ou superior; desenvolvimento em 21 |
| Plataforma nativa | Windows x64, cliente e servidor |
| Fabric API | Necessária |
| Controlify | Opcional; integração desenvolvida com 2.4.4+1.21.1 |
| Mod Menu | Opcional; integração desenvolvida com 11.0.5 |
| Simply Swords | Integração por classes de armas; necessário apenas para seus itens |
| VulkanMod | Opcional e externo, não incluído no mod; verificações locais com 0.6.7+1.21.1 |
| Outros mods de armas/armaduras | Detecção e adaptação com ajustes manuais; sem garantia universal |
| Servidor/LAN | Testes automáticos aprovados; sessão visual em duas máquinas ainda pendente |
| Forge/NeoForge | Não portado |

Os testes headless de geometria/renderização não equivalem a uma certificação visual em todas as GPUs ou combinações de mods. Mods que substituem o renderer do jogador ou o combate podem precisar de integração específica.

## Instalação e servidor

Use a mesma versão do Elden Flow em todos os clientes e no servidor, junto de Fabric API. Mods que acrescentam itens precisam estar presentes conforme os requisitos próprios deles. Não instale Controlify, Mod Menu ou VulkanMod no servidor dedicado apenas por causa do Elden Flow.

Veja [servidor e LAN](docs/SERVER.md), [validação realizada](docs/VALIDATION.md) e [compilação e dados locais](docs/BUILD.md). JNA é incluído pelo build; VulkanMod não é incorporado.

## Código, dados e distribuição

Este repositório contém código-fonte e recursos próprios. Os arquivos de animação e tabelas extraídos do jogo, DLLs proprietárias, saves e arquivos privados não fazem parte do pacote de código. Uma compilação de testes com dados sintéticos não reproduz as animações reais da prévia local. Consulte as instruções de build antes de tentar executá-la.

O balanceamento de dano, almas, atributos, passivas e adaptação à física de Minecraft é próprio do projeto; não se promete uma reprodução exata de todas as regras de Elden Ring.

Projeto de fã, sem afiliação ou endosso de Mojang/Microsoft, FromSoftware ou Bandai Namco. As capturas de tela serão adicionadas após a seleção pelo autor.
