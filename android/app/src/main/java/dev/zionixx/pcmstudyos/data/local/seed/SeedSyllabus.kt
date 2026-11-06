package dev.zionixx.pcmstudyos.data.local.seed

/**
 * Editable Class 11 CBSE seed syllabus.
 *
 * Source: CBSE Senior School Curriculum 2025-26 (cbseacademic.nic.in) and the
 * current NCERT textbooks, following the rationalised chapter list (for example
 * "Physical World" is no longer part of Class 11 Physics).
 *
 * This is seed data, not a locked curriculum: every chapter, topic and subtopic can
 * be renamed, reordered or deleted in the app. Check it against your school's
 * split-up before relying on it. Informatics Practices (065) is deliberately not
 * merged into Computer Science — add it as its own subject if you take it.
 */
object SeedSyllabus {

    data class SeedTopic(val name: String, val subtopics: List<String> = emptyList())
    data class SeedChapter(val name: String, val topics: List<SeedTopic>)
    data class SeedSubject(
        val name: String,
        val description: String,
        val color: String,
        val components: List<String>,
        val chapters: List<SeedChapter>,
    )

    val subjects: List<SeedSubject> = listOf(
        SeedSubject(
            name = "Physics",
            description = "CBSE Class 11 Physics (042) — rationalised syllabus, 2025-26.",
            color = "#4F8CFF",
            components = listOf("theory", "formulas", "ncert", "numericals", "questions"),
            chapters = listOf(
                SeedChapter(
                    name = "Units and Measurements",
                    topics = listOf(
                        SeedTopic("Units of measurement", listOf(
                            "SI base units",
                            "Fundamental and derived units",
                            "Unit consistency",
                        )),
                        SeedTopic("Significant figures", listOf(
                            "Rules for significant figures",
                            "Rounding off",
                            "Significant figures in calculations",
                        )),
                        SeedTopic("Dimensions of physical quantities", listOf(
                            "Dimensional formulae",
                            "Dimensional analysis",
                            "Checking dimensional consistency",
                        )),
                        SeedTopic("Applications of dimensional analysis", listOf(
                            "Deriving relations among physical quantities",
                            "Converting units between systems",
                            "Limitations of dimensional analysis",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Motion in a Straight Line",
                    topics = listOf(
                        SeedTopic("Frame of reference and position", listOf(
                            "Position and displacement",
                            "Path length vs displacement",
                        )),
                        SeedTopic("Speed and velocity", listOf(
                            "Average and instantaneous speed",
                            "Average and instantaneous velocity",
                        )),
                        SeedTopic("Uniformly accelerated motion", listOf(
                            "Acceleration",
                            "Equations of motion",
                            "Graphical treatment",
                        )),
                        SeedTopic("Kinematic graphs", listOf(
                            "Position-time graphs",
                            "Velocity-time graphs",
                            "Area under velocity-time graph",
                        )),
                        SeedTopic("Differentiation and integration in kinematics", listOf(
                            "Rate of change of position",
                            "Area under curves",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Motion in a Plane",
                    topics = listOf(
                        SeedTopic("Scalars and vectors", listOf(
                            "Equality of vectors",
                            "Multiplication by a real number",
                            "Addition and subtraction",
                            "Resolution of a vector",
                            "Unit vectors",
                        )),
                        SeedTopic("Vector products", listOf(
                            "Scalar product and properties",
                            "Vector product and properties",
                            "Applications of vector products",
                        )),
                        SeedTopic("Projectile motion", listOf(
                            "Horizontal projection",
                            "Oblique projection",
                            "Equation of trajectory",
                            "Maximum height, range and time of flight",
                        )),
                        SeedTopic("Uniform circular motion", listOf(
                            "Angular displacement and angular velocity",
                            "Centripetal acceleration",
                            "Relation between linear and angular speed",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Laws of Motion",
                    topics = listOf(
                        SeedTopic("Newton’s laws of motion", listOf(
                            "Inertia and the first law",
                            "Momentum and the second law",
                            "Impulse",
                            "Third law and action-reaction pairs",
                        )),
                        SeedTopic("Conservation of linear momentum", listOf(
                            "Momentum conservation",
                            "Recoil of a gun",
                            "Equilibrium of concurrent forces",
                        )),
                        SeedTopic("Friction", listOf(
                            "Static and kinetic friction",
                            "Laws of friction",
                            "Rolling friction",
                            "Lubrication",
                        )),
                        SeedTopic("Dynamics of uniform circular motion", listOf(
                            "Centripetal force",
                            "Vehicle on a level circular road",
                            "Banked road",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Work, Energy and Power",
                    topics = listOf(
                        SeedTopic("Work", listOf(
                            "Work done by a constant force",
                            "Work done by a variable force",
                            "Work-energy theorem",
                        )),
                        SeedTopic("Kinetic and potential energy", listOf(
                            "Kinetic energy",
                            "Potential energy of a spring",
                            "Conservative forces",
                            "Conservation of mechanical energy",
                        )),
                        SeedTopic("Power", listOf(
                            "Average and instantaneous power",
                            "Efficiency",
                        )),
                        SeedTopic("Collisions", listOf(
                            "Elastic collisions in one dimension",
                            "Inelastic collisions",
                            "Collisions in two dimensions",
                        )),
                        SeedTopic("Motion in a vertical circle", listOf(
                            "Condition for looping the loop",
                            "Tension at different points",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "System of Particles and Rotational Motion",
                    topics = listOf(
                        SeedTopic("Centre of mass", listOf(
                            "Centre of mass of a two-particle system",
                            "Centre of mass of a rigid body",
                            "Motion of centre of mass",
                        )),
                        SeedTopic("Torque and angular momentum", listOf(
                            "Moment of a force",
                            "Angular momentum",
                            "Conservation of angular momentum",
                            "Equilibrium of rigid bodies",
                        )),
                        SeedTopic("Rotational motion", listOf(
                            "Equations of rotational motion",
                            "Comparison of linear and rotational motion",
                        )),
                        SeedTopic("Moment of inertia", listOf(
                            "Moment of inertia and radius of gyration",
                            "Values for simple geometrical objects",
                            "Parallel and perpendicular axis theorems",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Gravitation",
                    topics = listOf(
                        SeedTopic("Kepler’s laws", listOf(
                            "Law of orbits",
                            "Law of areas",
                            "Law of periods",
                        )),
                        SeedTopic("Universal law of gravitation", listOf(
                            "Statement and vector form",
                            "Superposition principle",
                        )),
                        SeedTopic("Acceleration due to gravity", listOf(
                            "Variation with altitude",
                            "Variation with depth",
                            "Gravitational field intensity",
                        )),
                        SeedTopic("Gravitational potential and energy", listOf(
                            "Gravitational potential",
                            "Gravitational potential energy",
                            "Escape speed",
                        )),
                        SeedTopic("Satellites", listOf(
                            "Orbital velocity",
                            "Time period of a satellite",
                            "Energy of an orbiting satellite",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Mechanical Properties of Solids",
                    topics = listOf(
                        SeedTopic("Elastic behaviour of solids", listOf(
                            "Stress and strain",
                            "Stress-strain curve",
                            "Elastic hysteresis",
                        )),
                        SeedTopic("Hooke’s law and elastic moduli", listOf(
                            "Young’s modulus",
                            "Bulk modulus",
                            "Shear modulus and modulus of rigidity",
                            "Poisson’s ratio",
                        )),
                        SeedTopic("Elastic potential energy", listOf(
                            "Energy stored in a stretched wire",
                            "Applications of elastic behaviour",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Mechanical Properties of Fluids",
                    topics = listOf(
                        SeedTopic("Pressure in a fluid", listOf(
                            "Pressure due to a fluid column",
                            "Pascal’s law",
                            "Hydraulic lift and hydraulic brakes",
                            "Effect of gravity on fluid pressure",
                        )),
                        SeedTopic("Viscosity", listOf(
                            "Coefficient of viscosity",
                            "Stokes’ law",
                            "Terminal velocity",
                        )),
                        SeedTopic("Flow of fluids", listOf(
                            "Streamline and turbulent flow",
                            "Critical velocity",
                            "Equation of continuity",
                            "Bernoulli’s theorem and applications",
                        )),
                        SeedTopic("Surface tension", listOf(
                            "Surface energy",
                            "Angle of contact",
                            "Excess pressure across a curved surface",
                            "Drops, bubbles and capillary rise",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Thermal Properties of Matter",
                    topics = listOf(
                        SeedTopic("Heat and temperature", listOf(
                            "Thermal expansion of solids, liquids and gases",
                            "Anomalous expansion of water",
                        )),
                        SeedTopic("Specific heat capacity and calorimetry", listOf(
                            "Specific heat capacity",
                            "Calorimetry",
                            "Change of state and latent heat",
                        )),
                        SeedTopic("Heat transfer", listOf(
                            "Conduction and thermal conductivity",
                            "Convection",
                            "Radiation",
                            "Blackbody radiation",
                        )),
                        SeedTopic("Radiation laws", listOf(
                            "Wien’s displacement law",
                            "Stefan’s law",
                            "Newton’s law of cooling",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Thermodynamics",
                    topics = listOf(
                        SeedTopic("Thermal equilibrium", listOf(
                            "Zeroth law of thermodynamics",
                            "Definition of temperature",
                            "Heat, work and internal energy",
                        )),
                        SeedTopic("First law of thermodynamics", listOf(
                            "Statement and sign conventions",
                            "Isothermal process",
                            "Adiabatic process",
                            "Cyclic process and work done",
                        )),
                        SeedTopic("Second law of thermodynamics", listOf(
                            "Reversible and irreversible processes",
                            "Heat engines and efficiency",
                            "Carnot engine",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Kinetic Theory",
                    topics = listOf(
                        SeedTopic("Equation of state", listOf(
                            "Ideal gas equation",
                            "Work done in compressing a gas",
                            "Real gases and deviations",
                        )),
                        SeedTopic("Kinetic theory of gases", listOf(
                            "Assumptions of kinetic theory",
                            "Concept of pressure",
                            "Kinetic interpretation of temperature",
                        )),
                        SeedTopic("Molecular speeds and degrees of freedom", listOf(
                            "RMS speed",
                            "Degrees of freedom",
                            "Law of equipartition of energy",
                        )),
                        SeedTopic("Specific heats and mean free path", listOf(
                            "Specific heat capacities of gases",
                            "Mean free path",
                            "Avogadro’s number",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Oscillations",
                    topics = listOf(
                        SeedTopic("Periodic motion", listOf(
                            "Time period and frequency",
                            "Displacement as a function of time",
                            "Periodic functions",
                        )),
                        SeedTopic("Simple harmonic motion", listOf(
                            "SHM equation and phase",
                            "Velocity and acceleration in SHM",
                            "Graphical representation",
                        )),
                        SeedTopic("Oscillations of a loaded spring", listOf(
                            "Restoring force and force constant",
                            "Energy in SHM",
                            "Kinetic and potential energies",
                        )),
                        SeedTopic("Simple pendulum", listOf(
                            "Derivation of time period",
                            "Factors affecting time period",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Waves",
                    topics = listOf(
                        SeedTopic("Wave motion", listOf(
                            "Transverse and longitudinal waves",
                            "Speed of a travelling wave",
                            "Displacement relation for a progressive wave",
                        )),
                        SeedTopic("Superposition of waves", listOf(
                            "Principle of superposition",
                            "Reflection of waves",
                        )),
                        SeedTopic("Standing waves", listOf(
                            "Standing waves in strings",
                            "Standing waves in organ pipes",
                            "Fundamental mode and harmonics",
                        )),
                        SeedTopic("Beats", listOf(
                            "Formation of beats",
                            "Beat frequency",
                            "Applications",
                        )),
                    ),
                ),
            ),
        ),
        SeedSubject(
            name = "Chemistry",
            description = "CBSE Class 11 Chemistry (043) — rationalised syllabus, 2025-26.",
            color = "#39C6A4",
            components = listOf("theory", "formulas", "ncert", "numericals", "questions"),
            chapters = listOf(
                SeedChapter(
                    name = "Some Basic Concepts of Chemistry",
                    topics = listOf(
                        SeedTopic("Nature of matter", listOf(
                            "Elements, compounds and mixtures",
                            "Laws of chemical combination",
                            "Dalton’s atomic theory",
                        )),
                        SeedTopic("Atomic and molecular masses", listOf(
                            "Atomic mass unit",
                            "Average atomic mass",
                            "Molecular mass and formula mass",
                        )),
                        SeedTopic("Mole concept", listOf(
                            "Mole and Avogadro number",
                            "Molar mass",
                            "Percentage composition",
                            "Empirical and molecular formula",
                        )),
                        SeedTopic("Stoichiometry", listOf(
                            "Balanced chemical equations",
                            "Limiting reagent",
                            "Calculations based on stoichiometry",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Structure of Atom",
                    topics = listOf(
                        SeedTopic("Discovery of subatomic particles", listOf(
                            "Electron, proton and neutron",
                            "Atomic number",
                            "Isotopes and isobars",
                        )),
                        SeedTopic("Atomic models", listOf(
                            "Thomson’s model",
                            "Rutherford’s model",
                            "Bohr’s model and limitations",
                        )),
                        SeedTopic("Dual nature of matter and light", listOf(
                            "Wave nature of light",
                            "de Broglie relationship",
                            "Heisenberg uncertainty principle",
                        )),
                        SeedTopic("Quantum mechanical model", listOf(
                            "Concept of orbitals",
                            "Quantum numbers",
                            "Shapes of s, p and d orbitals",
                        )),
                        SeedTopic("Electronic configuration", listOf(
                            "Aufbau principle",
                            "Pauli exclusion principle",
                            "Hund’s rule",
                            "Stability of half-filled and filled orbitals",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Classification of Elements and Periodicity in Properties",
                    topics = listOf(
                        SeedTopic("Development of the periodic table", listOf(
                            "Brief history of classification",
                            "Modern periodic law",
                            "Present form of the periodic table",
                        )),
                        SeedTopic("Periodic trends", listOf(
                            "Atomic and ionic radii",
                            "Inert gas radii",
                            "Ionisation enthalpy",
                            "Electron gain enthalpy",
                            "Electronegativity and valency",
                        )),
                        SeedTopic("Nomenclature", listOf(
                            "IUPAC nomenclature of elements with atomic number above 100",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Chemical Bonding and Molecular Structure",
                    topics = listOf(
                        SeedTopic("Ionic and covalent bonds", listOf(
                            "Valence electrons",
                            "Ionic bond and lattice enthalpy",
                            "Covalent bond and bond parameters",
                        )),
                        SeedTopic("Lewis structures and resonance", listOf(
                            "Lewis dot structures",
                            "Formal charge",
                            "Resonance",
                            "Polar character of covalent bonds",
                        )),
                        SeedTopic("VSEPR theory", listOf(
                            "Geometry of covalent molecules",
                            "Shapes with lone pairs",
                            "Bond angles",
                        )),
                        SeedTopic("Hybridisation", listOf(
                            "s, p and d hybridisation",
                            "Examples and geometry",
                            "Limitations",
                        )),
                        SeedTopic("Molecular orbital theory", listOf(
                            "Bonding and antibonding orbitals",
                            "Homonuclear diatomic molecules",
                            "Bond order and magnetic behaviour",
                        )),
                        SeedTopic("Hydrogen bonding", listOf(
                            "Intermolecular hydrogen bonding",
                            "Intramolecular hydrogen bonding",
                            "Consequences",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Thermodynamics",
                    topics = listOf(
                        SeedTopic("Basic concepts", listOf(
                            "System and surroundings",
                            "Types of systems",
                            "Extensive and intensive properties",
                            "State functions",
                        )),
                        SeedTopic("First law of thermodynamics", listOf(
                            "Internal energy",
                            "Enthalpy",
                            "Measurement of ΔU and ΔH",
                            "Hess’s law",
                        )),
                        SeedTopic("Enthalpies of reaction", listOf(
                            "Bond dissociation enthalpy",
                            "Enthalpy of combustion and formation",
                            "Enthalpy of atomisation and sublimation",
                            "Enthalpy of ionisation, solution and dilution",
                        )),
                        SeedTopic("Second and third law", listOf(
                            "Entropy as a state function",
                            "Gibbs energy change",
                            "Spontaneity criteria",
                            "Third law of thermodynamics",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Equilibrium",
                    topics = listOf(
                        SeedTopic("Chemical equilibrium", listOf(
                            "Dynamic nature of equilibrium",
                            "Law of mass action",
                            "Equilibrium constant",
                            "Relation between Kp and Kc",
                        )),
                        SeedTopic("Le Chatelier’s principle", listOf(
                            "Effect of concentration",
                            "Effect of pressure and volume",
                            "Effect of temperature",
                            "Effect of catalyst",
                        )),
                        SeedTopic("Ionic equilibrium", listOf(
                            "Ionisation of acids and bases",
                            "Strong and weak electrolytes",
                            "Degree of ionisation",
                            "Acid strength and pH",
                        )),
                        SeedTopic("Buffers and solubility product", listOf(
                            "Hydrolysis of salts",
                            "Buffer solutions",
                            "Henderson equation",
                            "Solubility product and common ion effect",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Redox Reactions",
                    topics = listOf(
                        SeedTopic("Oxidation and reduction", listOf(
                            "Classical and electronic concepts",
                            "Oxidation number",
                            "Types of redox reactions",
                        )),
                        SeedTopic("Balancing redox reactions", listOf(
                            "Oxidation number method",
                            "Half-reaction method",
                            "Redox titrations",
                        )),
                        SeedTopic("Applications of redox reactions", listOf(
                            "Electrochemical cells (introduction)",
                            "Redox in daily life",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Organic Chemistry: Some Basic Principles and Techniques",
                    topics = listOf(
                        SeedTopic("Classification and nomenclature", listOf(
                            "Classification of organic compounds",
                            "IUPAC nomenclature",
                            "Common names",
                        )),
                        SeedTopic("Purification and analysis", listOf(
                            "Methods of purification",
                            "Qualitative analysis",
                            "Quantitative analysis",
                        )),
                        SeedTopic("Electronic displacements", listOf(
                            "Inductive effect",
                            "Electromeric effect",
                            "Resonance",
                            "Hyperconjugation",
                        )),
                        SeedTopic("Fission and reaction intermediates", listOf(
                            "Homolytic and heterolytic fission",
                            "Free radicals, carbocations and carbanions",
                            "Electrophiles and nucleophiles",
                            "Types of organic reactions",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Hydrocarbons",
                    topics = listOf(
                        SeedTopic("Classification of hydrocarbons", listOf(
                            "Saturated and unsaturated",
                            "Aromatic and aliphatic",
                        )),
                        SeedTopic("Alkanes", listOf(
                            "Nomenclature and isomerism",
                            "Conformations of ethane",
                            "Physical properties",
                            "Chemical reactions",
                        )),
                        SeedTopic("Alkenes", listOf(
                            "Nomenclature and structure",
                            "Geometrical isomerism",
                            "Methods of preparation",
                            "Chemical reactions",
                        )),
                        SeedTopic("Alkynes", listOf(
                            "Nomenclature and structure",
                            "Methods of preparation",
                            "Chemical reactions",
                        )),
                        SeedTopic("Aromatic hydrocarbons", listOf(
                            "IUPAC nomenclature",
                            "Benzene: resonance and aromaticity",
                            "Electrophilic substitution mechanism",
                            "Directive influence of substituents",
                            "Carcinogenicity and toxicity",
                        )),
                    ),
                ),
            ),
        ),
        SeedSubject(
            name = "Mathematics",
            description = "CBSE Class 11 Mathematics (041) — rationalised syllabus, 2025-26.",
            color = "#F2A03D",
            components = listOf("theory", "formulas", "ncert", "numericals", "questions"),
            chapters = listOf(
                SeedChapter(
                    name = "Sets",
                    topics = listOf(
                        SeedTopic("Sets and their representations", listOf(
                            "Roster form",
                            "Set-builder form",
                            "Types of sets",
                        )),
                        SeedTopic("Operations on sets", listOf(
                            "Union and intersection",
                            "Difference and complement",
                            "Venn diagrams",
                        )),
                        SeedTopic("Cardinal number of sets", listOf(
                            "Number of elements",
                            "Problems on union and intersection",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Relations and Functions",
                    topics = listOf(
                        SeedTopic("Cartesian product", listOf(
                            "Ordered pairs",
                            "Cartesian product of two sets",
                        )),
                        SeedTopic("Relations", listOf(
                            "Domain and range",
                            "Types of relations",
                        )),
                        SeedTopic("Functions", listOf(
                            "Domain, codomain and range",
                            "Real valued functions",
                            "Algebra of functions",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Trigonometric Functions",
                    topics = listOf(
                        SeedTopic("Angles and trigonometric ratios", listOf(
                            "Degree and radian measure",
                            "Signs of trigonometric functions",
                        )),
                        SeedTopic("Trigonometric identities", listOf(
                            "Fundamental identities",
                            "Sum and difference formulae",
                            "Double and multiple angle formulae",
                        )),
                        SeedTopic("Trigonometric equations", listOf(
                            "General solutions",
                            "Principal solutions",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Complex Numbers and Quadratic Equations",
                    topics = listOf(
                        SeedTopic("Complex numbers", listOf(
                            "Algebra of complex numbers",
                            "Modulus and argument",
                            "Conjugate",
                        )),
                        SeedTopic("Argand plane", listOf(
                            "Geometrical representation",
                            "Polar form",
                        )),
                        SeedTopic("Quadratic equations", listOf(
                            "Nature of roots",
                            "Discriminant",
                            "Solution of quadratic equations",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Linear Inequalities",
                    topics = listOf(
                        SeedTopic("Linear inequalities in one variable", listOf(
                            "Algebraic solutions",
                            "Graphical representation on a number line",
                        )),
                        SeedTopic("Linear inequalities in two variables", listOf(
                            "Graphical solution",
                            "System of inequalities",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Permutations and Combinations",
                    topics = listOf(
                        SeedTopic("Fundamental principle of counting", listOf(
                            "Multiplication principle",
                            "Addition principle",
                        )),
                        SeedTopic("Permutations", listOf(
                            "Permutations of distinct objects",
                            "Permutations with repetition",
                            "Circular permutations",
                        )),
                        SeedTopic("Combinations", listOf(
                            "Combinations and relation with permutations",
                            "Simple applications",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Binomial Theorem",
                    topics = listOf(
                        SeedTopic("Binomial theorem for positive integral index", listOf(
                            "Expansion",
                            "General and middle term",
                        )),
                        SeedTopic("Applications", listOf(
                            "Finding particular terms",
                            "Simple problems",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Sequences and Series",
                    topics = listOf(
                        SeedTopic("Sequences", listOf(
                            "Sequence and series",
                            "Arithmetic progression",
                            "Geometric progression",
                        )),
                        SeedTopic("Means and sums", listOf(
                            "Arithmetic mean",
                            "Geometric mean",
                            "Sum of n terms",
                            "Sum of special series",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Straight Lines",
                    topics = listOf(
                        SeedTopic("Slope and equations of a line", listOf(
                            "Slope of a line",
                            "Various forms of the equation of a line",
                            "Distance of a point from a line",
                        )),
                        SeedTopic("Family of lines", listOf(
                            "Parallel and perpendicular lines",
                            "Angle between two lines",
                            "Distance between parallel lines",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Conic Sections",
                    topics = listOf(
                        SeedTopic("Circle", listOf(
                            "Standard equation",
                            "General equation",
                        )),
                        SeedTopic("Parabola", listOf(
                            "Standard equations",
                            "Focus, directrix and latus rectum",
                        )),
                        SeedTopic("Ellipse", listOf(
                            "Standard equation",
                            "Eccentricity and latus rectum",
                        )),
                        SeedTopic("Hyperbola", listOf(
                            "Standard equation",
                            "Eccentricity and latus rectum",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Introduction to Three Dimensional Geometry",
                    topics = listOf(
                        SeedTopic("Coordinate axes and planes", listOf(
                            "Coordinate system in space",
                            "Coordinates of a point",
                        )),
                        SeedTopic("Distance and section formula", listOf(
                            "Distance between two points",
                            "Section formula",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Limits and Derivatives",
                    topics = listOf(
                        SeedTopic("Limits", listOf(
                            "Intuitive idea of limits",
                            "Algebra of limits",
                            "Standard limits",
                        )),
                        SeedTopic("Derivatives", listOf(
                            "Derivative as rate of change",
                            "Derivative from first principles",
                            "Algebra of derivatives",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Statistics",
                    topics = listOf(
                        SeedTopic("Measures of dispersion", listOf(
                            "Range",
                            "Mean deviation",
                            "Variance and standard deviation",
                        )),
                        SeedTopic("Analysis of frequency distributions", listOf(
                            "Comparison of distributions",
                            "Coefficient of variation",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Probability",
                    topics = listOf(
                        SeedTopic("Random experiments", listOf(
                            "Sample space",
                            "Events and types of events",
                        )),
                        SeedTopic("Probability of an event", listOf(
                            "Axiomatic approach",
                            "Addition rule",
                            "Simple problems",
                        )),
                    ),
                ),
            ),
        ),
        SeedSubject(
            name = "English Core",
            description = "CBSE Class 11 English Core (301) — Hornbill, Snapshots and skills.",
            color = "#C084FC",
            components = listOf("theory", "ncert", "questions"),
            chapters = listOf(
                SeedChapter(
                    name = "Hornbill — Prose",
                    topics = listOf(
                        SeedTopic("The Portrait of a Lady", listOf(
                            "Theme and narrative voice",
                            "Character of the grandmother",
                            "Important extracts",
                        )),
                        SeedTopic("We’re Not Afraid to Die... if We Can All Be Together", listOf(
                            "Plot and setting",
                            "Character analysis",
                            "Themes of courage and optimism",
                        )),
                        SeedTopic("Discovering Tut: The Saga Continues", listOf(
                            "Historical context",
                            "Themes",
                            "Important questions",
                        )),
                        SeedTopic("The Adventure", listOf(
                            "Plot and narrative technique",
                            "Themes",
                            "Important questions",
                        )),
                        SeedTopic("Silk Road", listOf(
                            "Travel narrative",
                            "Descriptive techniques",
                            "Important questions",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Hornbill — Poetry",
                    topics = listOf(
                        SeedTopic("A Photograph", listOf(
                            "Theme and imagery",
                            "Poetic devices",
                        )),
                        SeedTopic("The Laburnum Top", listOf(
                            "Symbolism",
                            "Poetic devices",
                        )),
                        SeedTopic("The Voice of the Rain", listOf(
                            "Personification",
                            "Theme",
                        )),
                        SeedTopic("Childhood", listOf(
                            "Theme and tone",
                            "Poetic devices",
                        )),
                        SeedTopic("Father to Son", listOf(
                            "Theme",
                            "Poetic devices",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Snapshots — Supplementary Reader",
                    topics = listOf(
                        SeedTopic("The Summer of the Beautiful White Horse", listOf(
                            "Theme and characters",
                            "Important questions",
                        )),
                        SeedTopic("The Address", listOf(
                            "Theme and narrative",
                            "Important questions",
                        )),
                        SeedTopic("Mother’s Day", listOf(
                            "Drama and satire",
                            "Character analysis",
                        )),
                        SeedTopic("Birth", listOf(
                            "Theme and characters",
                            "Important questions",
                        )),
                        SeedTopic("The Tale of Melon City", listOf(
                            "Satire",
                            "Theme",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Reading Skills",
                    topics = listOf(
                        SeedTopic("Unseen passages", listOf(
                            "Comprehension strategies",
                            "Note-making and summarising",
                        )),
                        SeedTopic("Factual and discursive passages", listOf(
                            "Inference questions",
                            "Vocabulary in context",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Writing Skills",
                    topics = listOf(
                        SeedTopic("Notice writing", listOf(
                            "Format and marking scheme",
                        )),
                        SeedTopic("Poster and advertisement", listOf(
                            "Format and layout",
                        )),
                        SeedTopic("Invitation and replies", listOf(
                            "Formal and informal",
                        )),
                        SeedTopic("Letters", listOf(
                            "Business and official letters",
                            "Letter to the editor",
                        )),
                        SeedTopic("Article and speech", listOf(
                            "Structure and language",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Grammar",
                    topics = listOf(
                        SeedTopic("Tenses and sentence structure", listOf(
                            "Gap filling",
                            "Sentence reordering",
                        )),
                        SeedTopic("Editing and omission", listOf(
                            "Error correction",
                        )),
                    ),
                ),
            ),
        ),
        SeedSubject(
            name = "Computer Science",
            description = "CBSE Class 11 Computer Science (083). Informatics Practices (065) can be added as a separate custom subject.",
            color = "#38BDF8",
            components = listOf("theory", "questions"),
            chapters = listOf(
                SeedChapter(
                    name = "Computer Systems and Organisation",
                    topics = listOf(
                        SeedTopic("Basic computer organisation", listOf(
                            "CPU, memory and input/output",
                            "Fetch-decode-execute cycle",
                        )),
                        SeedTopic("Types of software", listOf(
                            "System software",
                            "Application software",
                            "Utilities",
                        )),
                        SeedTopic("Boolean logic", listOf(
                            "Logic gates",
                            "Truth tables",
                            "Basic theorems",
                        )),
                        SeedTopic("Number systems and encoding", listOf(
                            "Binary, octal and hexadecimal",
                            "Conversions",
                            "ASCII and Unicode",
                        )),
                        SeedTopic("Emerging trends", listOf(
                            "Cloud computing",
                            "AI and ML",
                            "IoT and blockchain",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Computational Thinking and Programming — 1",
                    topics = listOf(
                        SeedTopic("Python basics", listOf(
                            "Variables and data types",
                            "Operators and expressions",
                            "Input and output",
                        )),
                        SeedTopic("Flow of control", listOf(
                            "Conditional statements",
                            "Loops",
                            "break and continue",
                        )),
                        SeedTopic("Functions", listOf(
                            "Defining and calling functions",
                            "Arguments and return values",
                            "Scope",
                        )),
                        SeedTopic("Strings", listOf(
                            "String operations",
                            "Slicing",
                            "String methods",
                        )),
                        SeedTopic("Lists and tuples", listOf(
                            "List operations",
                            "Nested lists",
                            "Tuples and immutability",
                        )),
                        SeedTopic("Dictionaries", listOf(
                            "Key-value pairs",
                            "Dictionary methods",
                            "Iteration",
                        )),
                        SeedTopic("File handling", listOf(
                            "Text files",
                            "Reading and writing",
                            "with statement",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Data Management — 1",
                    topics = listOf(
                        SeedTopic("Database concepts", listOf(
                            "Need for a database",
                            "Relational model",
                            "Keys",
                        )),
                        SeedTopic("SQL basics", listOf(
                            "DDL and DML",
                            "SELECT with WHERE",
                            "Aggregate functions",
                        )),
                        SeedTopic("Joins and constraints", listOf(
                            "Simple joins",
                            "Primary and foreign keys",
                        )),
                    ),
                ),
                SeedChapter(
                    name = "Society, Law and Ethics",
                    topics = listOf(
                        SeedTopic("Cyber safety", listOf(
                            "Safe browsing",
                            "Identity protection",
                            "Cyber crime",
                        )),
                        SeedTopic("Digital footprint and IPR", listOf(
                            "Digital rights",
                            "Licences",
                            "Plagiarism",
                        )),
                        SeedTopic("Technology and society", listOf(
                            "E-waste",
                            "Health impacts",
                            "Accessibility",
                        )),
                    ),
                ),
            ),
        ),
    )
}
