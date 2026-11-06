import type { ComponentType } from '../../types'

/** Class 11 CBSE seed syllabus.
 *
 * Source: CBSE Senior School Curriculum 2025-26 (cbseacademic.nic.in) and the
 * current NCERT textbooks. Chapter names follow the rationalised syllabus (for
 * example "Physical World" is no longer part of Class 11 Physics).
 *
 * This is EDITABLE seed data, not a locked curriculum: every chapter, topic and
 * subtopic can be renamed, reordered or deleted in the app. Always confirm
 * against your own school's split-up before relying on it. */

export interface SeedTopic {
  name: string
  subtopics?: string[]
}

export interface SeedChapter {
  name: string
  topics: SeedTopic[]
}

export interface SeedSubject {
  name: string
  description: string
  color: string
  icon: string
  components: ComponentType[]
  chapters: SeedChapter[]
}

const t = (name: string, subtopics?: string[]): SeedTopic => ({ name, subtopics })

const PCM_COMPONENTS: ComponentType[] = ['theory', 'formulas', 'ncert', 'numericals', 'questions']

export const CBSE11_SUBJECTS: SeedSubject[] = [
  {
    name: 'Physics',
    description: 'CBSE Class 11 Physics (042) — rationalised syllabus, 2025-26.',
    color: '#4f8cff',
    icon: 'atom',
    components: PCM_COMPONENTS,
    chapters: [
      {
        name: 'Units and Measurements',
        topics: [
          t('Units of measurement', ['SI base units', 'Fundamental and derived units', 'Unit consistency']),
          t('Significant figures', ['Rules for significant figures', 'Rounding off', 'Significant figures in calculations']),
          t('Dimensions of physical quantities', ['Dimensional formulae', 'Dimensional analysis', 'Checking dimensional consistency']),
          t('Applications of dimensional analysis', ['Deriving relations among physical quantities', 'Converting units between systems', 'Limitations of dimensional analysis']),
        ],
      },
      {
        name: 'Motion in a Straight Line',
        topics: [
          t('Frame of reference and position', ['Position and displacement', 'Path length vs displacement']),
          t('Speed and velocity', ['Average and instantaneous speed', 'Average and instantaneous velocity']),
          t('Uniformly accelerated motion', ['Acceleration', 'Equations of motion', 'Graphical treatment']),
          t('Kinematic graphs', ['Position-time graphs', 'Velocity-time graphs', 'Area under velocity-time graph']),
          t('Differentiation and integration in kinematics', ['Rate of change of position', 'Area under curves']),
        ],
      },
      {
        name: 'Motion in a Plane',
        topics: [
          t('Scalars and vectors', ['Equality of vectors', 'Multiplication by a real number', 'Addition and subtraction', 'Resolution of a vector', 'Unit vectors']),
          t('Vector products', ['Scalar product and properties', 'Vector product and properties', 'Applications of vector products']),
          t('Projectile motion', ['Horizontal projection', 'Oblique projection', 'Equation of trajectory', 'Maximum height, range and time of flight']),
          t('Uniform circular motion', ['Angular displacement and angular velocity', 'Centripetal acceleration', 'Relation between linear and angular speed']),
        ],
      },
      {
        name: 'Laws of Motion',
        topics: [
          t('Newton\u2019s laws of motion', ['Inertia and the first law', 'Momentum and the second law', 'Impulse', 'Third law and action-reaction pairs']),
          t('Conservation of linear momentum', ['Momentum conservation', 'Recoil of a gun', 'Equilibrium of concurrent forces']),
          t('Friction', ['Static and kinetic friction', 'Laws of friction', 'Rolling friction', 'Lubrication']),
          t('Dynamics of uniform circular motion', ['Centripetal force', 'Vehicle on a level circular road', 'Banked road']),
        ],
      },
      {
        name: 'Work, Energy and Power',
        topics: [
          t('Work', ['Work done by a constant force', 'Work done by a variable force', 'Work-energy theorem']),
          t('Kinetic and potential energy', ['Kinetic energy', 'Potential energy of a spring', 'Conservative forces', 'Conservation of mechanical energy']),
          t('Power', ['Average and instantaneous power', 'Efficiency']),
          t('Collisions', ['Elastic collisions in one dimension', 'Inelastic collisions', 'Collisions in two dimensions']),
          t('Motion in a vertical circle', ['Condition for looping the loop', 'Tension at different points']),
        ],
      },
      {
        name: 'System of Particles and Rotational Motion',
        topics: [
          t('Centre of mass', ['Centre of mass of a two-particle system', 'Centre of mass of a rigid body', 'Motion of centre of mass']),
          t('Torque and angular momentum', ['Moment of a force', 'Angular momentum', 'Conservation of angular momentum', 'Equilibrium of rigid bodies']),
          t('Rotational motion', ['Equations of rotational motion', 'Comparison of linear and rotational motion']),
          t('Moment of inertia', ['Moment of inertia and radius of gyration', 'Values for simple geometrical objects', 'Parallel and perpendicular axis theorems']),
        ],
      },
      {
        name: 'Gravitation',
        topics: [
          t('Kepler\u2019s laws', ['Law of orbits', 'Law of areas', 'Law of periods']),
          t('Universal law of gravitation', ['Statement and vector form', 'Superposition principle']),
          t('Acceleration due to gravity', ['Variation with altitude', 'Variation with depth', 'Gravitational field intensity']),
          t('Gravitational potential and energy', ['Gravitational potential', 'Gravitational potential energy', 'Escape speed']),
          t('Satellites', ['Orbital velocity', 'Time period of a satellite', 'Energy of an orbiting satellite']),
        ],
      },
      {
        name: 'Mechanical Properties of Solids',
        topics: [
          t('Elastic behaviour of solids', ['Stress and strain', 'Stress-strain curve', 'Elastic hysteresis']),
          t('Hooke\u2019s law and elastic moduli', ['Young\u2019s modulus', 'Bulk modulus', 'Shear modulus and modulus of rigidity', 'Poisson\u2019s ratio']),
          t('Elastic potential energy', ['Energy stored in a stretched wire', 'Applications of elastic behaviour']),
        ],
      },
      {
        name: 'Mechanical Properties of Fluids',
        topics: [
          t('Pressure in a fluid', ['Pressure due to a fluid column', 'Pascal\u2019s law', 'Hydraulic lift and hydraulic brakes', 'Effect of gravity on fluid pressure']),
          t('Viscosity', ['Coefficient of viscosity', 'Stokes\u2019 law', 'Terminal velocity']),
          t('Flow of fluids', ['Streamline and turbulent flow', 'Critical velocity', 'Equation of continuity', 'Bernoulli\u2019s theorem and applications']),
          t('Surface tension', ['Surface energy', 'Angle of contact', 'Excess pressure across a curved surface', 'Drops, bubbles and capillary rise']),
        ],
      },
      {
        name: 'Thermal Properties of Matter',
        topics: [
          t('Heat and temperature', ['Thermal expansion of solids, liquids and gases', 'Anomalous expansion of water']),
          t('Specific heat capacity and calorimetry', ['Specific heat capacity', 'Calorimetry', 'Change of state and latent heat']),
          t('Heat transfer', ['Conduction and thermal conductivity', 'Convection', 'Radiation', 'Blackbody radiation']),
          t('Radiation laws', ['Wien\u2019s displacement law', 'Stefan\u2019s law', 'Newton\u2019s law of cooling']),
        ],
      },
      {
        name: 'Thermodynamics',
        topics: [
          t('Thermal equilibrium', ['Zeroth law of thermodynamics', 'Definition of temperature', 'Heat, work and internal energy']),
          t('First law of thermodynamics', ['Statement and sign conventions', 'Isothermal process', 'Adiabatic process', 'Cyclic process and work done']),
          t('Second law of thermodynamics', ['Reversible and irreversible processes', 'Heat engines and efficiency', 'Carnot engine']),
        ],
      },
      {
        name: 'Kinetic Theory',
        topics: [
          t('Equation of state', ['Ideal gas equation', 'Work done in compressing a gas', 'Real gases and deviations']),
          t('Kinetic theory of gases', ['Assumptions of kinetic theory', 'Concept of pressure', 'Kinetic interpretation of temperature']),
          t('Molecular speeds and degrees of freedom', ['RMS speed', 'Degrees of freedom', 'Law of equipartition of energy']),
          t('Specific heats and mean free path', ['Specific heat capacities of gases', 'Mean free path', 'Avogadro\u2019s number']),
        ],
      },
      {
        name: 'Oscillations',
        topics: [
          t('Periodic motion', ['Time period and frequency', 'Displacement as a function of time', 'Periodic functions']),
          t('Simple harmonic motion', ['SHM equation and phase', 'Velocity and acceleration in SHM', 'Graphical representation']),
          t('Oscillations of a loaded spring', ['Restoring force and force constant', 'Energy in SHM', 'Kinetic and potential energies']),
          t('Simple pendulum', ['Derivation of time period', 'Factors affecting time period']),
        ],
      },
      {
        name: 'Waves',
        topics: [
          t('Wave motion', ['Transverse and longitudinal waves', 'Speed of a travelling wave', 'Displacement relation for a progressive wave']),
          t('Superposition of waves', ['Principle of superposition', 'Reflection of waves']),
          t('Standing waves', ['Standing waves in strings', 'Standing waves in organ pipes', 'Fundamental mode and harmonics']),
          t('Beats', ['Formation of beats', 'Beat frequency', 'Applications']),
        ],
      },
    ],
  },
  {
    name: 'Chemistry',
    description: 'CBSE Class 11 Chemistry (043) — rationalised syllabus, 2025-26.',
    color: '#39c6a4',
    icon: 'flask',
    components: PCM_COMPONENTS,
    chapters: [
      {
        name: 'Some Basic Concepts of Chemistry',
        topics: [
          t('Nature of matter', ['Elements, compounds and mixtures', 'Laws of chemical combination', 'Dalton\u2019s atomic theory']),
          t('Atomic and molecular masses', ['Atomic mass unit', 'Average atomic mass', 'Molecular mass and formula mass']),
          t('Mole concept', ['Mole and Avogadro number', 'Molar mass', 'Percentage composition', 'Empirical and molecular formula']),
          t('Stoichiometry', ['Balanced chemical equations', 'Limiting reagent', 'Calculations based on stoichiometry']),
        ],
      },
      {
        name: 'Structure of Atom',
        topics: [
          t('Discovery of subatomic particles', ['Electron, proton and neutron', 'Atomic number', 'Isotopes and isobars']),
          t('Atomic models', ['Thomson\u2019s model', 'Rutherford\u2019s model', 'Bohr\u2019s model and limitations']),
          t('Dual nature of matter and light', ['Wave nature of light', 'de Broglie relationship', 'Heisenberg uncertainty principle']),
          t('Quantum mechanical model', ['Concept of orbitals', 'Quantum numbers', 'Shapes of s, p and d orbitals']),
          t('Electronic configuration', ['Aufbau principle', 'Pauli exclusion principle', 'Hund\u2019s rule', 'Stability of half-filled and filled orbitals']),
        ],
      },
      {
        name: 'Classification of Elements and Periodicity in Properties',
        topics: [
          t('Development of the periodic table', ['Brief history of classification', 'Modern periodic law', 'Present form of the periodic table']),
          t('Periodic trends', ['Atomic and ionic radii', 'Inert gas radii', 'Ionisation enthalpy', 'Electron gain enthalpy', 'Electronegativity and valency']),
          t('Nomenclature', ['IUPAC nomenclature of elements with atomic number above 100']),
        ],
      },
      {
        name: 'Chemical Bonding and Molecular Structure',
        topics: [
          t('Ionic and covalent bonds', ['Valence electrons', 'Ionic bond and lattice enthalpy', 'Covalent bond and bond parameters']),
          t('Lewis structures and resonance', ['Lewis dot structures', 'Formal charge', 'Resonance', 'Polar character of covalent bonds']),
          t('VSEPR theory', ['Geometry of covalent molecules', 'Shapes with lone pairs', 'Bond angles']),
          t('Hybridisation', ['s, p and d hybridisation', 'Examples and geometry', 'Limitations']),
          t('Molecular orbital theory', ['Bonding and antibonding orbitals', 'Homonuclear diatomic molecules', 'Bond order and magnetic behaviour']),
          t('Hydrogen bonding', ['Intermolecular hydrogen bonding', 'Intramolecular hydrogen bonding', 'Consequences']),
        ],
      },
      {
        name: 'Thermodynamics',
        topics: [
          t('Basic concepts', ['System and surroundings', 'Types of systems', 'Extensive and intensive properties', 'State functions']),
          t('First law of thermodynamics', ['Internal energy', 'Enthalpy', 'Measurement of ΔU and ΔH', 'Hess\u2019s law']),
          t('Enthalpies of reaction', ['Bond dissociation enthalpy', 'Enthalpy of combustion and formation', 'Enthalpy of atomisation and sublimation', 'Enthalpy of ionisation, solution and dilution']),
          t('Second and third law', ['Entropy as a state function', 'Gibbs energy change', 'Spontaneity criteria', 'Third law of thermodynamics']),
        ],
      },
      {
        name: 'Equilibrium',
        topics: [
          t('Chemical equilibrium', ['Dynamic nature of equilibrium', 'Law of mass action', 'Equilibrium constant', 'Relation between Kp and Kc']),
          t('Le Chatelier\u2019s principle', ['Effect of concentration', 'Effect of pressure and volume', 'Effect of temperature', 'Effect of catalyst']),
          t('Ionic equilibrium', ['Ionisation of acids and bases', 'Strong and weak electrolytes', 'Degree of ionisation', 'Acid strength and pH']),
          t('Buffers and solubility product', ['Hydrolysis of salts', 'Buffer solutions', 'Henderson equation', 'Solubility product and common ion effect']),
        ],
      },
      {
        name: 'Redox Reactions',
        topics: [
          t('Oxidation and reduction', ['Classical and electronic concepts', 'Oxidation number', 'Types of redox reactions']),
          t('Balancing redox reactions', ['Oxidation number method', 'Half-reaction method', 'Redox titrations']),
          t('Applications of redox reactions', ['Electrochemical cells (introduction)', 'Redox in daily life']),
        ],
      },
      {
        name: 'Organic Chemistry: Some Basic Principles and Techniques',
        topics: [
          t('Classification and nomenclature', ['Classification of organic compounds', 'IUPAC nomenclature', 'Common names']),
          t('Purification and analysis', ['Methods of purification', 'Qualitative analysis', 'Quantitative analysis']),
          t('Electronic displacements', ['Inductive effect', 'Electromeric effect', 'Resonance', 'Hyperconjugation']),
          t('Fission and reaction intermediates', ['Homolytic and heterolytic fission', 'Free radicals, carbocations and carbanions', 'Electrophiles and nucleophiles', 'Types of organic reactions']),
        ],
      },
      {
        name: 'Hydrocarbons',
        topics: [
          t('Classification of hydrocarbons', ['Saturated and unsaturated', 'Aromatic and aliphatic']),
          t('Alkanes', ['Nomenclature and isomerism', 'Conformations of ethane', 'Physical properties', 'Chemical reactions']),
          t('Alkenes', ['Nomenclature and structure', 'Geometrical isomerism', 'Methods of preparation', 'Chemical reactions']),
          t('Alkynes', ['Nomenclature and structure', 'Methods of preparation', 'Chemical reactions']),
          t('Aromatic hydrocarbons', ['IUPAC nomenclature', 'Benzene: resonance and aromaticity', 'Electrophilic substitution mechanism', 'Directive influence of substituents', 'Carcinogenicity and toxicity']),
        ],
      },
    ],
  },
  {
    name: 'Mathematics',
    description: 'CBSE Class 11 Mathematics (041) — rationalised syllabus, 2025-26.',
    color: '#f2a03d',
    icon: 'sigma',
    components: PCM_COMPONENTS,
    chapters: [
      {
        name: 'Sets',
        topics: [
          t('Sets and their representations', ['Roster form', 'Set-builder form', 'Types of sets']),
          t('Operations on sets', ['Union and intersection', 'Difference and complement', 'Venn diagrams']),
          t('Cardinal number of sets', ['Number of elements', 'Problems on union and intersection']),
        ],
      },
      {
        name: 'Relations and Functions',
        topics: [
          t('Cartesian product', ['Ordered pairs', 'Cartesian product of two sets']),
          t('Relations', ['Domain and range', 'Types of relations']),
          t('Functions', ['Domain, codomain and range', 'Real valued functions', 'Algebra of functions']),
        ],
      },
      {
        name: 'Trigonometric Functions',
        topics: [
          t('Angles and trigonometric ratios', ['Degree and radian measure', 'Signs of trigonometric functions']),
          t('Trigonometric identities', ['Fundamental identities', 'Sum and difference formulae', 'Double and multiple angle formulae']),
          t('Trigonometric equations', ['General solutions', 'Principal solutions']),
        ],
      },
      {
        name: 'Complex Numbers and Quadratic Equations',
        topics: [
          t('Complex numbers', ['Algebra of complex numbers', 'Modulus and argument', 'Conjugate']),
          t('Argand plane', ['Geometrical representation', 'Polar form']),
          t('Quadratic equations', ['Nature of roots', 'Discriminant', 'Solution of quadratic equations']),
        ],
      },
      {
        name: 'Linear Inequalities',
        topics: [
          t('Linear inequalities in one variable', ['Algebraic solutions', 'Graphical representation on a number line']),
          t('Linear inequalities in two variables', ['Graphical solution', 'System of inequalities']),
        ],
      },
      {
        name: 'Permutations and Combinations',
        topics: [
          t('Fundamental principle of counting', ['Multiplication principle', 'Addition principle']),
          t('Permutations', ['Permutations of distinct objects', 'Permutations with repetition', 'Circular permutations']),
          t('Combinations', ['Combinations and relation with permutations', 'Simple applications']),
        ],
      },
      {
        name: 'Binomial Theorem',
        topics: [
          t('Binomial theorem for positive integral index', ['Expansion', 'General and middle term']),
          t('Applications', ['Finding particular terms', 'Simple problems']),
        ],
      },
      {
        name: 'Sequences and Series',
        topics: [
          t('Sequences', ['Sequence and series', 'Arithmetic progression', 'Geometric progression']),
          t('Means and sums', ['Arithmetic mean', 'Geometric mean', 'Sum of n terms', 'Sum of special series']),
        ],
      },
      {
        name: 'Straight Lines',
        topics: [
          t('Slope and equations of a line', ['Slope of a line', 'Various forms of the equation of a line', 'Distance of a point from a line']),
          t('Family of lines', ['Parallel and perpendicular lines', 'Angle between two lines', 'Distance between parallel lines']),
        ],
      },
      {
        name: 'Conic Sections',
        topics: [
          t('Circle', ['Standard equation', 'General equation']),
          t('Parabola', ['Standard equations', 'Focus, directrix and latus rectum']),
          t('Ellipse', ['Standard equation', 'Eccentricity and latus rectum']),
          t('Hyperbola', ['Standard equation', 'Eccentricity and latus rectum']),
        ],
      },
      {
        name: 'Introduction to Three Dimensional Geometry',
        topics: [
          t('Coordinate axes and planes', ['Coordinate system in space', 'Coordinates of a point']),
          t('Distance and section formula', ['Distance between two points', 'Section formula']),
        ],
      },
      {
        name: 'Limits and Derivatives',
        topics: [
          t('Limits', ['Intuitive idea of limits', 'Algebra of limits', 'Standard limits']),
          t('Derivatives', ['Derivative as rate of change', 'Derivative from first principles', 'Algebra of derivatives']),
        ],
      },
      {
        name: 'Statistics',
        topics: [
          t('Measures of dispersion', ['Range', 'Mean deviation', 'Variance and standard deviation']),
          t('Analysis of frequency distributions', ['Comparison of distributions', 'Coefficient of variation']),
        ],
      },
      {
        name: 'Probability',
        topics: [
          t('Random experiments', ['Sample space', 'Events and types of events']),
          t('Probability of an event', ['Axiomatic approach', 'Addition rule', 'Simple problems']),
        ],
      },
    ],
  },
  {
    name: 'English Core',
    description: 'CBSE Class 11 English Core (301) — Hornbill, Snapshots and skills.',
    color: '#c084fc',
    icon: 'book',
    components: ['theory', 'ncert', 'questions'],
    chapters: [
      {
        name: 'Hornbill — Prose',
        topics: [
          t('The Portrait of a Lady', ['Theme and narrative voice', 'Character of the grandmother', 'Important extracts']),
          t('We\u2019re Not Afraid to Die... if We Can All Be Together', ['Plot and setting', 'Character analysis', 'Themes of courage and optimism']),
          t('Discovering Tut: The Saga Continues', ['Historical context', 'Themes', 'Important questions']),
          t('The Adventure', ['Plot and narrative technique', 'Themes', 'Important questions']),
          t('Silk Road', ['Travel narrative', 'Descriptive techniques', 'Important questions']),
        ],
      },
      {
        name: 'Hornbill — Poetry',
        topics: [
          t('A Photograph', ['Theme and imagery', 'Poetic devices']),
          t('The Laburnum Top', ['Symbolism', 'Poetic devices']),
          t('The Voice of the Rain', ['Personification', 'Theme']),
          t('Childhood', ['Theme and tone', 'Poetic devices']),
          t('Father to Son', ['Theme', 'Poetic devices']),
        ],
      },
      {
        name: 'Snapshots — Supplementary Reader',
        topics: [
          t('The Summer of the Beautiful White Horse', ['Theme and characters', 'Important questions']),
          t('The Address', ['Theme and narrative', 'Important questions']),
          t('Mother\u2019s Day', ['Drama and satire', 'Character analysis']),
          t('Birth', ['Theme and characters', 'Important questions']),
          t('The Tale of Melon City', ['Satire', 'Theme']),
        ],
      },
      {
        name: 'Reading Skills',
        topics: [
          t('Unseen passages', ['Comprehension strategies', 'Note-making and summarising']),
          t('Factual and discursive passages', ['Inference questions', 'Vocabulary in context']),
        ],
      },
      {
        name: 'Writing Skills',
        topics: [
          t('Notice writing', ['Format and marking scheme']),
          t('Poster and advertisement', ['Format and layout']),
          t('Invitation and replies', ['Formal and informal']),
          t('Letters', ['Business and official letters', 'Letter to the editor']),
          t('Article and speech', ['Structure and language']),
        ],
      },
      {
        name: 'Grammar',
        topics: [
          t('Tenses and sentence structure', ['Gap filling', 'Sentence reordering']),
          t('Editing and omission', ['Error correction']),
        ],
      },
    ],
  },
  {
    name: 'Computer Science',
    description: 'CBSE Class 11 Computer Science (083). Informatics Practices (065) can be added as a separate custom subject.',
    color: '#38bdf8',
    icon: 'code',
    components: ['theory', 'questions'],
    chapters: [
      {
        name: 'Computer Systems and Organisation',
        topics: [
          t('Basic computer organisation', ['CPU, memory and input/output', 'Fetch-decode-execute cycle']),
          t('Types of software', ['System software', 'Application software', 'Utilities']),
          t('Boolean logic', ['Logic gates', 'Truth tables', 'Basic theorems']),
          t('Number systems and encoding', ['Binary, octal and hexadecimal', 'Conversions', 'ASCII and Unicode']),
          t('Emerging trends', ['Cloud computing', 'AI and ML', 'IoT and blockchain']),
        ],
      },
      {
        name: 'Computational Thinking and Programming — 1',
        topics: [
          t('Python basics', ['Variables and data types', 'Operators and expressions', 'Input and output']),
          t('Flow of control', ['Conditional statements', 'Loops', 'break and continue']),
          t('Functions', ['Defining and calling functions', 'Arguments and return values', 'Scope']),
          t('Strings', ['String operations', 'Slicing', 'String methods']),
          t('Lists and tuples', ['List operations', 'Nested lists', 'Tuples and immutability']),
          t('Dictionaries', ['Key-value pairs', 'Dictionary methods', 'Iteration']),
          t('File handling', ['Text files', 'Reading and writing', 'with statement']),
        ],
      },
      {
        name: 'Data Management — 1',
        topics: [
          t('Database concepts', ['Need for a database', 'Relational model', 'Keys']),
          t('SQL basics', ['DDL and DML', 'SELECT with WHERE', 'Aggregate functions']),
          t('Joins and constraints', ['Simple joins', 'Primary and foreign keys']),
        ],
      },
      {
        name: 'Society, Law and Ethics',
        topics: [
          t('Cyber safety', ['Safe browsing', 'Identity protection', 'Cyber crime']),
          t('Digital footprint and IPR', ['Digital rights', 'Licences', 'Plagiarism']),
          t('Technology and society', ['E-waste', 'Health impacts', 'Accessibility']),
        ],
      },
    ],
  },
]
