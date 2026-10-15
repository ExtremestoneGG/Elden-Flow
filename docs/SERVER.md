# Elden Flow — servidor e LAN

O mesmo JAR é instalado no cliente e no servidor. Esta implementação nativa é para Windows x64, Minecraft 1.21.1, Java 21 e Fabric. Linux/macOS ainda não estão incluídos. Todos devem usar a mesma versão do Elden Flow.

No servidor dedicado, instale Fabric API e o Elden Flow na pasta mods. Instale também os mods que fornecem os itens usados pelo mundo, como Simply Swords e suas dependências. Controlify, Mod Menu e VulkanMod são opcionais de cliente; não são necessários para iniciar o servidor. JNA já está incluído no JAR do Elden Flow.

Na LAN, abra o mundo normalmente com o mod e use a opção do Minecraft para abrir em LAN. O computador anfitrião aplica as mesmas regras autoritativas do servidor dedicado.

## Configurações do mundo

- eldenflow-weapons.json: classes, posições, rotações, escalas, encaixes por postura e permissão de parry de cada item.
- eldenflow-progression.json: almas e atributos persistidos por UUID.
- eldenflow-stamina.json: quando existir, preserva os bônus antigos de estamina.

Esses arquivos ficam dentro da pasta do mundo. Faça backup do mundo antes de editar arquivos manualmente e edite com o servidor desligado. Um mundo novo copia o preset inicial de config/eldensteve-weapons.json; depois sua configuração é independente.

Somente anfitrião ou jogador com permissão de operador nível 2 pode salvar/importar configurações pelo editor. Visitantes recebem as regras em partes, aplicadas somente após a transferência completa. O preset pessoal do visitante volta a valer ao desconectar.

O servidor decide dano, estamina, defesa, parry, acertos, almas e compras. Clientes enviam comandos, não valores de dano ou saldo. Compras são revalidadas junto à fogueira; remover a laje, apagar a fogueira, entrar na água ou interromper descanso revoga a compra. No criativo, compras não consomem o saldo de sobrevivência.

## Validação

Os testes automatizados usam um servidor Minecraft real com jogadores simulados conectados, sem abrir janela. Isso verifica protocolo, isolamento entre jogadores, persistência, permissões e combate. Não substitui o teste visual de latência e renderização com duas pessoas em computadores diferentes.
