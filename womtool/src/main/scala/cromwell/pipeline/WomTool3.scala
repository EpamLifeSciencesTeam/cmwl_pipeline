package cromwell.pipeline

import womtool.WomtoolMain.{ SuccessfulTermination, UnsuccessfulTermination }
import womtool.cmdline.ValidateCommandLine
import womtool.input.WomGraphMaker

object WomTool3 extends App {
//  val main = args(2)

//  commandLineArgs match {
//    case v: ValidateCommandLine => {
//      println(
//        "v.inputs=" + v.inputs + " v.workflowSource=" + v.workflowSource + " v.listDependencies=" + v.listDependencies
//      )
//      validate(v.workflowSource, v.inputs, v.listDependencies)
//    }
//  }

//  println("InputsNotDefined main=" + main)
//  WomGraphMaker.getBundle(main) match {
//    case Right(b)     => println("succ"); SuccessfulTermination(validationSuccessMsg(b.resolvedImportRecords))
//    case Left(errors) => println("unsucc"); UnsuccessfulTermination(errors.toList.mkString(System.lineSeparator))
//  }
}
