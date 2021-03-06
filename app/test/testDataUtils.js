import { intRange } from '~/src/arrayUtils'

export function createArea({
  areaNumber,
  hasMainDetails = false,
  additionalFields = {},
}) {
  const fields = {}
  if (hasMainDetails) {
    fields.description = `Test Area Description ${areaNumber}`
    fields.notes = `Test Area Notes ${areaNumber}`
    fields.links = [
      createLink({ itemNumber: areaNumber, linkNumber: 1 }),
      createLink({ itemNumber: areaNumber, linkNumber: 2 }),
    ]
  }
  return {
    id: `test-area-id-${areaNumber}`,
    name: `Test Area Name ${areaNumber}`,
    components: [],
    ...fields,
    ...additionalFields,
  }
}

export function createComponent({
  componentNumber,
  hasMainDetails = false,
  platformNumber,
  hasTechDebts = false,
  hasTestResults = false,
  additionalFields = {},
}) {
  const fields = {}
  if (hasMainDetails) {
    fields.description = `Test Component Description ${componentNumber}`
    fields.notes = `Test Component Notes ${componentNumber}`
    fields.links = [
      createLink({ itemNumber: componentNumber, linkNumber: 1 }),
      createLink({ itemNumber: componentNumber, linkNumber: 2 }),
    ]
    fields.responsibilities = [
      createResponsibility({ componentNumber, responsibilityNumber: 1 }),
      createResponsibility({ componentNumber, responsibilityNumber: 2 }),
    ]
    if (platformNumber === undefined) {
      platformNumber = componentNumber
    }
  }
  if (hasTechDebts) {
    fields.techDebts = createTechDebts(componentNumber)
  }
  if (hasTestResults) {
    fields.testResults = createTestResults(componentNumber)
  }
  const component = {
    id: `test-component-id-${componentNumber}`,
    name: `Test Component Name ${componentNumber}`,
    typeId: `test-component-type-id-${componentNumber}`,
    description: `Test Component Description ${componentNumber}`,
    tags: [`test-tag-${componentNumber}-1`, `test-tag-${componentNumber}-2`],
    repo: {
      url: `https://example.com/test-repo-${componentNumber}`,
    },
    teams: [
      {
        teamId: `test-team-id-${componentNumber}-1`,
      },
      {
        teamId: `test-team-id-${componentNumber}-2`,
      },
    ],
    ...fields,
    ...additionalFields,
  }
  if (platformNumber !== undefined) {
    component.platformId = `test-platform-id-${platformNumber}`
  }
  return component
}

function createCrossFunctionalRequirements(componentNumber) {
  return [
    createCrossFunctionalRequirement({
      componentNumber,
      crossFunctionalRequirementNumber: 1,
    }),
    createCrossFunctionalRequirement({
      componentNumber,
      crossFunctionalRequirementNumber: 2,
    }),
  ]
}

export function createComponentWithCrossFunctionalRequirements({
  componentNumber,
}) {
  return createComponent({
    componentNumber,
    additionalFields: {
      crossFunctionalRequirements:
        createCrossFunctionalRequirements(componentNumber),
    },
  })
}

export function createComponentWithGitRepo({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      gitRepo: createGitRepo({ componentNumber }),
    },
  })
}

function createImports(componentNumber) {
  return [
    createImport({ componentNumber, importNumber: 1 }),
    createImport({ componentNumber, importNumber: 2 }),
  ]
}

export function createComponentWithImports({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      imports: createImports(componentNumber),
    },
  })
}

export function createComponentWithLinesOfCode({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      linesOfCode: createLinesOfCode({ componentNumber, importNumber: 1 }),
    },
  })
}

function createOpenApiSpecs(componentNumber) {
  return [
    createOpenApiSpec({ componentNumber, openApiSpecNumber: 1 }),
    createOpenApiSpec({ componentNumber, openApiSpecNumber: 2 }),
  ]
}

export function createComponentWithOpenApiSpecs({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      openApiSpecs: createOpenApiSpecs(componentNumber),
    },
  })
}

export function createComponentWithReadme({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      readme: createReadme({ componentNumber }),
    },
  })
}

function createScannerErrors(componentNumber) {
  return [
    createScannerError({
      componentNumber,
      scannerErrorNumber: 1,
    }),
    createScannerError({
      componentNumber,
      scannerErrorNumber: 2,
    }),
  ]
}

export function createComponentWithScannerErrors({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      scannerErrors: createScannerErrors(componentNumber),
    },
  })
}

function CreateSoftwareItems(componentNumber) {
  return [
    createSoftwareItem({ componentNumber, softwareItemNumber: 1 }),
    createSoftwareItem({ componentNumber, softwareItemNumber: 2 }),
    createSoftwareItem({ componentNumber, softwareItemNumber: 3 }),
  ]
}

export function createComponentWithSoftwareItems({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      software: CreateSoftwareItems(componentNumber),
    },
  })
}

function createSoftwareRepositories(componentNumber) {
  return [
    createSoftwareRepository({
      componentNumber,
      softwareRepositoryNumber: 1,
    }),
    createSoftwareRepository({
      componentNumber,
      softwareRepositoryNumber: 2,
    }),
    createSoftwareRepository({
      componentNumber,
      softwareRepositoryNumber: 3,
    }),
  ]
}

export function createComponentWithSoftwareRepositories({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      softwareRepositories: createSoftwareRepositories(componentNumber),
    },
  })
}

function createTechDebts(componentNumber) {
  return [
    createTechDebt({
      componentNumber,
      techDebtNumber: 1,
    }),
    createTechDebt({
      componentNumber,
      techDebtNumber: 2,
    }),
  ]
}

export function createComponentWithTechDebts({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      techDebts: createTechDebts(componentNumber),
    },
  })
}

function createTestResults(componentNumber) {
  return [
    createTestResult({
      componentNumber,
      testResultNumber: 1,
    }),
    createTestResult({
      componentNumber,
      testResultNumber: 2,
    }),
  ]
}

export function createComponentWithTestResults({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      testResults: createTestResults(componentNumber),
    },
  })
}

function createToDos(componentNumber) {
  return [
    createToDo({ componentNumber, toDoNumber: 1 }),
    createToDo({ componentNumber, toDoNumber: 2 }),
    createToDo({ componentNumber, toDoNumber: 3 }),
  ]
}

export function createComponentWithToDos({ componentNumber }) {
  return createComponent({
    componentNumber,
    additionalFields: {
      toDos: createToDos(componentNumber),
    },
  })
}

export function createScanner({ scannerNumber, additionalFields = {} }) {
  return {
    id: `test-scanner-id-${scannerNumber}`,
    description: `Test Scanner Description ${scannerNumber}`,
    notes: `Test Scanner Notes ${scannerNumber}`,
    ...additionalFields,
  }
}

export function createTeam({
  teamNumber,
  areaNumber,
  hasMainDetails = false,
  additionalFields = {},
}) {
  if (areaNumber === undefined) {
    areaNumber = teamNumber
  }
  const fields = {}
  if (hasMainDetails) {
    fields.description = `Test Area Description ${areaNumber}`
    fields.notes = `Test Area Notes ${areaNumber}`
    fields.links = [
      createLink({ itemNumber: areaNumber, linkNumber: 1 }),
      createLink({ itemNumber: areaNumber, linkNumber: 2 }),
    ]
  }
  return {
    id: `test-team-id-${teamNumber}`,
    name: `Test Team Name ${teamNumber}`,
    emailAddress: `test-team-email-1ddress-${teamNumber}@example.com`,
    areaId: `test-area-id-${areaNumber}`,
    description: `Test Team Description ${teamNumber}`,
    links: [
      {
        url: `https://example.com/test-team-link-${teamNumber}-1`,
        description: `Test Team Link Description ${teamNumber} 1`,
      },
      {
        url: `https://example.com/test-team-link-${teamNumber}-2`,
        description: `Test Team Link Description ${teamNumber} 2`,
      },
    ],
    components: [],
    ...fields,
    ...additionalFields,
  }
}

export function createTest({ testNumber, additionalFields = {} }) {
  return {
    id: `test-test-id-${testNumber}`,
    description: `Test Test Description ${testNumber}`,
    notes: `Test Test Notes ${testNumber}`,
    priority: createPriority(testNumber),
    ...additionalFields,
  }
}

export function createSummaryWithEmptyComponentAndSubComponentDependencies() {
  return {
    componentDependencies: {
      nodes: [],
      dependencies: [],
    },
    subComponentDependencies: {
      nodes: [],
      dependencies: [],
    },
  }
}

function createCrossFunctionalRequirement({
  componentNumber,
  crossFunctionalRequirementNumber,
}) {
  return {
    description: `Test CFR Description ${componentNumber} ${crossFunctionalRequirementNumber}`,
    notes: `Test CFR Notes ${componentNumber} ${crossFunctionalRequirementNumber}`,
    links: [
      {
        url: `https://example.com/test-cfr-link-${componentNumber}-${crossFunctionalRequirementNumber}-1`,
        description: `Test CFR Link Description ${componentNumber} ${crossFunctionalRequirementNumber} 1`,
      },
      {
        url: `https://example.com/test-cfr-link-${componentNumber}-${crossFunctionalRequirementNumber}-2`,
        description: `Test CFR Link Description ${componentNumber} ${crossFunctionalRequirementNumber} 2`,
      },
    ],
  }
}

function createIdentity({ componentNumber, identityNumber }) {
  const dayOfMonth = `${componentNumber.toString().padStart(2, '0')}`
  return {
    names: createIdentityNames({ componentNumber, identityNumber }),
    emailAddress: `test-identity-${componentNumber}-${identityNumber}@example.com`,
    commitCount: identityNumber,
    firstCommitTimestamp: `1970-01-${dayOfMonth}T00:00:00.000Z`,
    lastCommitTimestamp: `1980-01-${dayOfMonth}T00:00:00.000Z`,
  }
}

function createIdentityNames({ componentNumber, identityNumber }) {
  return intRange(1, identityNumber + 1).map((identityNameNumber) => {
    return `Test Identity Name ${componentNumber} ${identityNumber} ${identityNameNumber}`
  })
}

function createLink({ itemNumber, linkNumber }) {
  return {
    url: `https://example.com/test-link-${itemNumber}-${linkNumber}`,
    description: `Test Link Description ${itemNumber} ${linkNumber}`,
  }
}

function createGitRepo({ componentNumber }) {
  const dayOfMonth = `${componentNumber.toString().padStart(2, '0')}`
  return {
    firstCommitTimestamp: `1970-01-${dayOfMonth}T00:00:00.000Z`,
    lastCommitTimestamp: `1980-01-${dayOfMonth}T00:00:00.000Z`,
    commitCount: 100 + componentNumber,
    authors: [
      createIdentity({ componentNumber, identityNumber: 1 }),
      createIdentity({ componentNumber, identityNumber: 2 }),
      createIdentity({ componentNumber, identityNumber: 3 }),
      createIdentity({ componentNumber, identityNumber: 4 }),
    ],
    committers: [
      createIdentity({ componentNumber, identityNumber: 1 }),
      createIdentity({ componentNumber, identityNumber: 2 }),
      createIdentity({ componentNumber, identityNumber: 3 }),
      createIdentity({ componentNumber, identityNumber: 4 }),
      createIdentity({ componentNumber, identityNumber: 5 }),
    ],
    authorCount: 4,
    committerCount: 5,
  }
}

function createImport({ componentNumber, importNumber }) {
  return {
    scannerId: `test-scanner-id-${componentNumber}-${importNumber}`,
    type: 'java',
    name: `test.import.Name_${componentNumber}_${importNumber}`,
  }
}

function createLinesOfCode({ componentNumber }) {
  return {
    count: componentNumber,
    fileExtensionCounts: [
      createFileExtensionCount({
        componentNumber,
        fileExtensionCountNumber: 1,
      }),
      createFileExtensionCount({
        componentNumber,
        fileExtensionCountNumber: 2,
      }),
      createFileExtensionCount({
        componentNumber,
        fileExtensionCountNumber: 3,
      }),
      createFileExtensionCount({
        componentNumber,
        fileExtensionCountNumber: 4,
      }),
      createFileExtensionCount({
        componentNumber,
        fileExtensionCountNumber: 5,
      }),
    ],
  }
}

function createFileExtensionCount({
  componentNumber,
  fileExtensionCountNumber,
}) {
  return {
    fileExtension: `extension-${fileExtensionCountNumber}`,
    count: 100 + componentNumber + fileExtensionCountNumber,
  }
}

function createOpenApiSpec({ componentNumber, openApiSpecNumber }) {
  return {
    spec: {},
    url: `https://example.com/test-openapi-spec-${componentNumber}-${openApiSpecNumber}`,
    description: `Test OpenAPI Spec Description ${componentNumber} ${openApiSpecNumber}`,
  }
}

function createPriority(itemNumber) {
  switch (itemNumber % 4) {
    case 0:
      return 'low'
    case 1:
      return 'medium'
    case 2:
      return 'high'
    case 3:
      return 'very-high'
  }
}

function createReadme({ componentNumber }) {
  return {
    fileName: `README-${componentNumber}.md`,
    content: `# Test Readme Content ${componentNumber}`,
  }
}

function createResponsibility({ componentNumber, responsibilityNumber }) {
  return {
    description: `Test Responsibility Description ${componentNumber} ${responsibilityNumber}`,
  }
}

function createScannerError({ componentNumber, scannerErrorNumber }) {
  return {
    scannerId: `test-scanner-id-${componentNumber}-${scannerErrorNumber}`,
    message: `Test Scanner Error Message ${componentNumber} ${scannerErrorNumber}`,
    cause: {
      message: `Test Scanner Error Message ${componentNumber} ${scannerErrorNumber} 2`,
    },
  }
}

function createSoftwareItem({ componentNumber, softwareItemNumber }) {
  return {
    scannerId: `test-scanner-id-${componentNumber}-${softwareItemNumber}`,
    type: `test-software-type-id-${componentNumber}-${softwareItemNumber}`,
    dependencyType: `test-software-dependency-type-${componentNumber}-${softwareItemNumber}`,
    name: `Test Software Name ${componentNumber} ${softwareItemNumber}`,
    version: `1.${componentNumber}.${softwareItemNumber}`,
    versionSelector: `^1.${componentNumber}.${softwareItemNumber}`,
    packaging: `test-software-packaging-${componentNumber}-${softwareItemNumber}`,
    scope: `test-software-scope-${componentNumber}-${softwareItemNumber}`,
  }
}

function createSoftwareRepository({
  componentNumber,
  softwareRepositoryNumber,
}) {
  return {
    scannerId: `test-scanner-id-${componentNumber}-${softwareRepositoryNumber}`,
    type: `test-software-repository-type-id-${componentNumber}-${softwareRepositoryNumber}`,
    url: `https://example.com/test-software-repository-url-${componentNumber}-${softwareRepositoryNumber}`,
    safe: softwareRepositoryNumber % 2 === 1,
    scope: `test-software-repository-scope-${componentNumber}-${softwareRepositoryNumber}`,
  }
}

export function createTechDebt({ componentNumber, techDebtNumber }) {
  return {
    description: `Test Tech Debt Description ${componentNumber} ${techDebtNumber}`,
    notes: `Test Tech Debt Notes ${componentNumber} ${techDebtNumber}`,
    priority: createPriority(techDebtNumber),
    links: [
      {
        url: `https://example.com/test-tech-debt-link-${componentNumber}-${techDebtNumber}-1`,
        description: `Test Tech Debt Link Description ${componentNumber} ${techDebtNumber} 1`,
      },
      {
        url: `https://example.com/test-tech-debt-link-${componentNumber}-${techDebtNumber}-2`,
        description: `Test Tech Debt Link Description ${componentNumber} ${techDebtNumber} 2`,
      },
    ],
  }
}

function createTestOutcome(itemNumber) {
  switch (itemNumber % 3) {
    case 0:
      return 'pass'
    case 1:
      return 'fail'
    case 2:
      return 'not-applicable'
  }
}

export function createTestResult({ componentNumber, testResultNumber }) {
  return {
    testId: `test-test-id-${componentNumber}-${testResultNumber}`,
    priority: createPriority(testResultNumber),
    outcome: createTestOutcome(testResultNumber),
    message: `Test Test Message ${componentNumber} ${testResultNumber}`,
  }
}

function createToDo({ componentNumber, toDoNumber }) {
  return {
    file: `test-to-do-file-${componentNumber}-${toDoNumber}.txt`,
    description: `Test To Do Description ${componentNumber} ${toDoNumber}`,
  }
}

export function createComponentDependencies() {
  return {
    nodes: [
      createComponentNode({ componentNodeNumber: 1 }),
      createComponentNode({ componentNodeNumber: 2 }),
      createComponentNode({ componentNodeNumber: 3 }),
    ],
    dependencies: [
      createDependency({ sourceIndex: 0, targetIndex: 1 }),
      createDependency({ sourceIndex: 1, targetIndex: 2 }),
    ],
  }
}

export function createSubComponentDependencies() {
  return {
    nodes: [
      createSubComponentNode({ componentNodeNumber: 1 }),
      createSubComponentNode({ componentNodeNumber: 2 }),
      createSubComponentNode({ componentNodeNumber: 3 }),
    ],
    dependencies: [
      createDependency({ sourceIndex: 0, targetIndex: 1 }),
      createDependency({ sourceIndex: 1, targetIndex: 2 }),
    ],
  }
}

export function createComponentNode({ componentNodeNumber }) {
  return {
    componentId: `test-component-id-${componentNodeNumber}`,
  }
}

export function createSubComponentNode({
  componentNodeNumber,
  subComponentNodeNumber,
}) {
  if (subComponentNodeNumber === undefined) {
    subComponentNodeNumber = componentNodeNumber
  }
  return {
    componentId: `test-component-id-${componentNodeNumber}`,
    spanName: `test-span-name-${subComponentNodeNumber}-${subComponentNodeNumber}`,
    tags: {},
  }
}

export function createDependency({ sourceIndex, targetIndex }) {
  return {
    sourceIndex,
    targetIndex,
    relatedIndexes: [],
    manual: false,
    sampleSize: 1,
    startTimestamp: '2021-01-01T00:00:00.000Z',
    endTimestamp: '2021-01-01T00:00:01.000Z',
    duration: {
      min: 1_000_000,
      max: 1_000_000,
      p50: 1_000_000,
      p90: 1_000_000,
      p99: 1_000_000,
      p99Point9: 1_000_000,
    },
  }
}
