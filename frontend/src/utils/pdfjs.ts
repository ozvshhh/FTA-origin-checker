/**
 * pdf.js 를 한 곳에서 설정한다 (뷰어·쪽 수 세기 공용).
 * 라이브러리가 커서(약 400kB) 첫 화면 묶음에 넣지 않고, PDF 를 처음 열거나 셀 때 불러온다.
 */
import type { PDFDocumentLoadingTask } from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'

let lib: Promise<typeof import('pdfjs-dist')> | null = null
const loadLib = () =>
  (lib ??= import('pdfjs-dist').then((m) => {
    m.GlobalWorkerOptions.workerSrc = workerUrl
    return m
  }))

/** PDF 열기 작업을 만든다. 끝나면(또는 그만두면) task.destroy() */
export async function openPdf(data: Uint8Array): Promise<PDFDocumentLoadingTask> {
  const { getDocument } = await loadLib()
  return getDocument({ data })
}

/** 브라우저에서 PDF 쪽 수를 센다. 열 수 없으면 null (서버가 다시 검사한다) */
export async function countPages(file: File): Promise<number | null> {
  try {
    const task = await openPdf(new Uint8Array(await file.arrayBuffer()))
    const doc = await task.promise
    const n = doc.numPages
    await task.destroy()
    return n
  } catch {
    return null
  }
}
