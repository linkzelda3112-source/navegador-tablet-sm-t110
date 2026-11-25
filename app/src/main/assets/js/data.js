/* Contenido del juego (todo en español). */
(function (root) {
  'use strict';
  var D = {};

  // [emoji, palabra separada en sílabas]
  var RAW_WORDS = [
    ['🐱', 'ga-to'], ['🐶', 'pe-rro'], ['🏠', 'ca-sa'], ['🦆', 'pa-to'], ['🌙', 'lu-na'],
    ['🍐', 'pe-ra'], ['🛏️', 'ca-ma'], ['📖', 'li-bro'], ['🌹', 'ro-sa'], ['☕', 'ta-za'],
    ['🧃', 'ju-go'], ['🥾', 'bo-ta'], ['🎲', 'da-do'], ['🛁', 'ba-ño'], ['🐻', 'o-so'],
    ['🍍', 'pi-ña'], ['🐺', 'lo-bo'], ['🐸', 'ra-na'], ['🐵', 'mo-no'], ['🔑', 'lla-ve'],
    ['🔥', 'fue-go'], ['🐂', 'to-ro'], ['🥥', 'co-co'], ['🗺️', 'ma-pa'], ['📦', 'ca-ja'],
    ['📏', 're-gla'], ['🍇', 'u-vas'], ['🍓', 'fre-sa'], ['🎹', 'pia-no'], ['🥁', 'tam-bor'],
    ['⌚', 're-loj'], ['🐟', 'pez'],
    ['⚽', 'pe-lo-ta'], ['🍅', 'to-ma-te'], ['👞', 'za-pa-to'], ['🐴', 'ca-ba-llo'], ['🍪', 'ga-lle-ta'],
    ['🐋', 'ba-lle-na'], ['🍦', 'he-la-do'], ['🐰', 'co-ne-jo'], ['🦒', 'ji-ra-fa'], ['👕', 'ca-mi-sa'],
    ['🐢', 'tor-tu-ga'], ['🔨', 'mar-ti-llo'], ['🎸', 'gui-ta-rra'], ['⌨️', 'te-cla-do'], ['🦘', 'can-gu-ro'],
    ['☂️', 'pa-ra-guas'], ['🐌', 'ca-ra-col'], ['🐜', 'hor-mi-ga'], ['🐧', 'pin-güi-no'], ['🔔', 'cam-pa-na'],
    ['🍎', 'man-za-na'], ['🍊', 'na-ran-ja'], ['🧅', 'ce-bo-lla'], ['🪐', 'pla-ne-ta'], ['⭐', 'es-tre-lla'],
    ['✂️', 'ti-je-ras'], ['🎒', 'mo-chi-la'], ['📱', 'ce-lu-lar'],
    ['🐘', 'e-le-fan-te'], ['🦋', 'ma-ri-po-sa'], ['🚲', 'bi-ci-cle-ta'], ['🥕', 'za-na-ho-ria'],
    ['✏️', 'lá-piz'], ['🌳', 'ár-bol'], ['🐦', 'pá-ja-ro'], ['🍌', 'plá-ta-no'], ['🍉', 'san-dí-a'],
    ['🍋', 'li-món'], ['🐉', 'dra-gón'], ['✈️', 'a-vión'], ['❤️', 'co-ra-zón'], ['🦇', 'mur-cié-la-go']
  ];

  D.WORDS = RAW_WORDS.map(function (r) {
    var syl = r[1].split('-');
    var w = syl.join('');
    return { emoji: r[0], word: w, syl: syl, accent: /[áéíóú]/.test(w) };
  });

  // Palabras con hueco para el Detective de letras.
  // [emoji, plantilla con _, letra(s) correcta(s), letras incorrectas]
  D.MIRROR = [
    ['🎲', '_ado', 'd', ['b']], ['🥾', '_ota', 'b', ['d']], ['👶', '_ebé', 'b', ['d']],
    ['👄', '_oca', 'b', ['d']], ['🦆', '_ato', 'p', ['q']], ['🧀', '_ueso', 'q', ['p']],
    ['🍐', '_era', 'p', ['q']], ['🚪', '_uerta', 'p', ['q']], ['🦷', '_iente', 'd', ['b']],
    ['🥁', 'tam_or', 'b', ['d']], ['☝️', '_edo', 'd', ['b']], ['🌙', 'lu_a', 'n', ['m']]
  ];

  D.ORTO = [
    ['🐄', '_aca', 'v', ['b']], ['🔑', '_ave', 'll', ['y']], ['🐋', '_allena', 'b', ['v']],
    ['🚲', '_icicleta', 'b', ['v']], ['🦒', '_irafa', 'j', ['g']], ['🐜', '_ormiga', 'h', ['—']],
    ['🧊', '_ielo', 'h', ['—']], ['🥕', '_anahoria', 'z', ['s']], ['🍪', '_alleta', 'g', ['j']],
    ['🐰', 'cone_o', 'j', ['g']], ['🐴', 'ca_allo', 'b', ['v']], ['🎸', '_uitarra', 'gu', ['g']],
    ['🐯', 'ti_re', 'g', ['j']], ['🍊', 'naran_a', 'j', ['g']], ['🕯️', '_ela', 'v', ['b']],
    ['🧅', 'ce_olla', 'b', ['v']], ['🍇', 'u_as', 'v', ['b']], ['🚢', '_arco', 'b', ['v']],
    ['🐝', 'a_eja', 'b', ['v']], ['⛵', '_elero', 'v', ['b']]
  ];

  D.SENTENCES = [
    'El gato duerme sobre el sillón azul.',
    'Mi abuela prepara tamales los domingos.',
    'Los niños juegan fútbol en el parque.',
    'La luna brilla mucho en la noche.',
    'Ayer vimos una película muy divertida.',
    'Mariana dibujó un dragón de colores.',
    'El perro corre detrás de la pelota roja.',
    'Mañana iremos a la playa con mis primos.',
    'En la escuela aprendemos cosas nuevas cada día.',
    'La mariposa se posó sobre una flor amarilla.',
    'Mi mamá me lee un cuento antes de dormir.',
    'Los pájaros cantan cuando sale el sol.',
    'Con paciencia y práctica todo se puede lograr.',
    'El mar estaba tranquilo y el cielo muy claro.'
  ];

  D.STORIES = [
    {
      id: 'zorro', emoji: '🦊', title: 'El zorro y la luna',
      sents: [
        'Una noche, un zorro pequeño vio la luna en el río.',
        'Pensó que era un queso enorme y brillante.',
        'Se acercó despacio y metió la pata en el agua.',
        'La luna se movió y se rompió en mil pedacitos.',
        'El zorro se asustó y retrocedió.',
        'Entonces miró hacia arriba y vio la luna en el cielo.',
        'Se rió de su error y volvió a casa feliz.'
      ],
      qs: [
        { q: '¿Qué creyó el zorro que era la luna?', o: ['Un queso', 'Una pelota', 'Una moneda'], a: 0, hint: 1 },
        { q: '¿Dónde vio primero la luna?', o: ['En el río', 'En una cueva', 'En un árbol'], a: 0, hint: 0 },
        { q: '¿Cómo terminó el zorro?', o: ['Enojado', 'Feliz', 'Dormido'], a: 1, hint: 6 }
      ]
    },
    {
      id: 'tortuga', emoji: '🐢', title: 'La tortuga Lola',
      sents: [
        'Lola era una tortuga muy lenta, pero muy constante.',
        'Un día, sus amigos organizaron una carrera hasta el lago.',
        'La liebre salió corriendo y pronto desapareció.',
        'Lola caminó paso a paso sin detenerse.',
        'La liebre se quedó dormida debajo de un árbol.',
        'Al despertar, vio que Lola ya estaba cruzando la meta.',
        'Lola aprendió que ir despacio también sirve si no te rindes.'
      ],
      qs: [
        { q: '¿Cómo era Lola?', o: ['Muy rápida', 'Lenta y constante', 'Muy dormilona'], a: 1, hint: 0 },
        { q: '¿Qué hizo la liebre?', o: ['Se quedó dormida', 'Se fue a nadar', 'Ayudó a Lola'], a: 0, hint: 4 },
        { q: '¿Qué aprendió Lola?', o: ['Que correr es malo', 'Que ir despacio sirve si no te rindes', 'Que los lagos son fríos'], a: 1, hint: 6 }
      ]
    },
    {
      id: 'robot', emoji: '🤖', title: 'El robot jardinero',
      sents: [
        'En una casa vivía un robot que cuidaba el jardín.',
        'Cada mañana regaba las flores con una manguera larga.',
        'Un día se acabó la batería y el robot se detuvo.',
        'Sofía, la niña de la casa, lo vio quieto entre las plantas.',
        'Corrió por el cargador y lo conectó con cuidado.',
        'El robot encendió sus luces y dijo gracias con un pitido.',
        'Desde entonces, Sofía y el robot cuidan juntos el jardín.'
      ],
      qs: [
        { q: '¿Qué hacía el robot cada mañana?', o: ['Cocinaba', 'Regaba las flores', 'Pintaba la casa'], a: 1, hint: 1 },
        { q: '¿Por qué se detuvo el robot?', o: ['Se acabó la batería', 'Se rompió una rueda', 'Empezó a llover'], a: 0, hint: 2 },
        { q: '¿Quién ayudó al robot?', o: ['Un vecino', 'Sofía', 'Su mamá'], a: 1, hint: 4 }
      ]
    },
    {
      id: 'galletas', emoji: '🍪', title: 'El misterio de las galletas',
      sents: [
        'Una tarde, Mateo llegó a la cocina y la jarra de galletas estaba vacía.',
        'Había migas en el suelo y huellas pequeñitas junto a la mesa.',
        'Mateo siguió las huellas hasta debajo del sillón.',
        'Allí encontró a Canela, la perrita, con la nariz llena de migas.',
        'Mateo no se enojó, solo se rió.',
        'Esa noche guardó las galletas en un lugar más alto.'
      ],
      qs: [
        { q: '¿Qué desapareció?', o: ['Las galletas', 'Los dulces', 'El pan'], a: 0, hint: 0 },
        { q: '¿Quién se las comió?', o: ['Su hermana', 'Canela, la perrita', 'Un ratón'], a: 1, hint: 3 },
        { q: '¿Dónde guardó Mateo las galletas después?', o: ['En el refrigerador', 'En un lugar más alto', 'Debajo del sillón'], a: 1, hint: 5 }
      ]
    },
    {
      id: 'faro', emoji: '🌊', title: 'El faro del pueblo',
      sents: [
        'Al final del pueblo había un faro viejo junto al mar.',
        'Don Ramiro era el encargado de encender su luz cada noche.',
        'Una noche hubo una tormenta muy fuerte.',
        'Los pescadores no veían el camino de regreso.',
        'Don Ramiro subió las escaleras y encendió el faro.',
        'La luz guio a todos los barcos hasta el puerto.',
        'Al día siguiente, el pueblo le agradeció con una gran fiesta.'
      ],
      qs: [
        { q: '¿Quién encendía el faro?', o: ['Don Ramiro', 'Un pescador', 'Un niño'], a: 0, hint: 1 },
        { q: '¿Qué pasó una noche?', o: ['Hubo una tormenta', 'Se perdió un gato', 'Hubo un eclipse'], a: 0, hint: 2 },
        { q: '¿Cómo agradeció el pueblo?', o: ['Con una carta', 'Con un viaje', 'Con una fiesta'], a: 2, hint: 6 }
      ]
    },
    {
      id: 'camila', emoji: '🚀', title: 'Camila y el planeta azul',
      sents: [
        'Camila soñaba con viajar al espacio desde que era muy pequeña.',
        'Un día construyó una nave con cajas de cartón.',
        'Se puso un casco, una capa brillante y empezó la cuenta regresiva.',
        'Tres, dos, uno... ¡despegue!',
        'En su imaginación llegó a un planeta azul lleno de estrellas.',
        'Allí conoció a unos extraterrestres amables que le enseñaron a bailar.',
        'Cuando su mamá la llamó a cenar, Camila volvió a casa sonriendo.'
      ],
      qs: [
        { q: '¿Con qué construyó Camila su nave?', o: ['Con cajas de cartón', 'Con madera', 'Con latas'], a: 0, hint: 1 },
        { q: '¿De qué color era el planeta?', o: ['Rojo', 'Azul', 'Verde'], a: 1, hint: 4 },
        { q: '¿Qué le enseñaron los extraterrestres?', o: ['A cocinar', 'A bailar', 'A volar'], a: 1, hint: 5 }
      ]
    }
  ];

  // Paquetes de práctica de trazo
  D.TRACE = {
    minus: { title: 'Letras pequeñas', emoji: 'a', items: 'abcdefghijklmnñopqrstuvwxyz'.split('') },
    mayus: { title: 'Letras grandes', emoji: 'A', items: 'ABCDEFGHIJKLMNÑOPQRSTUVWXYZ'.split('') },
    silabas: { title: 'Sílabas', emoji: 'ma', items: ['ma', 'me', 'mi', 'mo', 'mu', 'pa', 'pe', 'pi', 'po', 'pu', 'sa', 'se', 'si', 'so', 'su', 'la', 'le', 'li', 'lo', 'lu', 'ta', 'te', 'ti', 'to', 'tu', 'ca', 'co', 'cu', 'ra', 'ro'] },
    palabras: { title: 'Palabras', emoji: 'sol', items: ['sol', 'luna', 'mesa', 'casa', 'gato', 'pato', 'flor', 'mar', 'pan', 'rana', 'nube', 'lápiz', 'libro', 'mamá', 'papá', 'amigo', 'jugar', 'feliz'] }
  };

  D.STICKERS = ['🦄', '🐬', '🌈', '🦋', '🐙', '🍭', '🎈', '🐼', '🦊', '🌻', '🚀', '🎨',
    '🐱', '🦉', '🍓', '⚽', '🎸', '🐢', '🦁', '🌟', '🍩', '🐝', '🎁', '👑'];
  D.MASCOTS = ['🦊', '🐱', '🐼', '🦉', '🐸', '🦄'];

  D.PRAISE = ['¡Muy bien!', '¡Genial!', '¡Lo lograste!', '¡Excelente!', '¡Qué bien!', '¡Súper!', '¡Perfecto!'];
  D.RETRY = ['Casi. Inténtalo otra vez.', 'Tú puedes. Prueba otra vez.', 'Muy cerca. Otra vez.', 'No pasa nada. Intenta de nuevo.'];

  if (typeof module !== 'undefined' && module.exports) module.exports = D;
  else root.Data = D;
})(typeof window !== 'undefined' ? window : this);
