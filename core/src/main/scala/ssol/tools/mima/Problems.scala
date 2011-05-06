package ssol.tools.mima

object Problem {
  object Status extends Enumeration {
    val Unfixable = Value("unfixable")
    val Upgradable = Value("upgradable") // means MiMa Client can fix the bytecode
    //val SourceFixable = Value("source fixable") // means that the break may be easily fixed in the source
    val Ignored = Value("ignored")
  }

  object ClassVersion extends Enumeration {
    val New = Value("new")
    val Old = Value("old")
  }
}

sealed abstract class Problem {
  var status = Problem.Status.Unfixable
  val fileName: String
  val description: String
  val fixHint: Option[ui.FixHint] = None
}

case class MissingFieldProblem(oldfld: MemberInfo) extends Problem {
  override val fileName: String = oldfld.owner.sourceFileName
  override val description = oldfld.fieldString + " does not have a correspondent in new version"
}

case class MissingMethodProblem(meth: MemberInfo)(implicit affectedClassVersion: Problem.ClassVersion.Value = Problem.ClassVersion.New) extends Problem {
  override val fileName: String = meth.owner.sourceFileName
  override val description = (if (meth.isDeferred && !meth.owner.isTrait) "abstract " else "") + meth.methodString + " does not have a correspondent in " + affectedClassVersion + " version"
}

case class UpdateForwarderBodyProblem(meth: MemberInfo) extends Problem {
  assert(meth.owner.isTrait)
  assert(meth.owner.hasStaticImpl(meth))
  
  status = Problem.Status.Upgradable
  override val fileName: String = meth.owner.sourceFileName
  override val description = "classes mixing " + meth.owner.fullName + " needs to update body of " + meth.shortMethodString 
  
}

case class MissingClassProblem(oldclazz: ClassInfo) extends Problem {
  override val fileName: String = oldclazz.sourceFileName
  override val description = oldclazz.classString + " does not have a correspondent in new version"
}

case class InaccessibleFieldProblem(newfld: MemberInfo) extends Problem {
  override val fileName: String = newfld.owner.sourceFileName
  override val description = newfld.fieldString + " was public; is inaccessible in new version"
}

case class InaccessibleMethodProblem(newmeth: MemberInfo) extends Problem {
  override val fileName: String = newmeth.owner.sourceFileName
  override val description = newmeth.methodString + " was public; is inaccessible in new version"
}

case class InaccessibleClassProblem(newclazz: ClassInfo) extends Problem {
  override val fileName: String = newclazz.sourceFileName
  override val description = newclazz.classString + " was public; is inaccessible in new version"
}

case class IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo) extends Problem {
  override val fileName: String = oldfld.owner.sourceFileName
  override val description = newfld.fieldString + "'s type has changed; was: " + oldfld.tpe + ", is now: " + newfld.tpe
}

case class IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo]) extends Problem {
  override val fixHint = {
    if(!oldmeth.hasSyntheticName) {
       newmeths.find(_.params.size == oldmeth.params.size) match {
         case None => None
         case Some(meth) => Some(ui.AddBridgeMethod(oldmeth, meth))
       }
    }
    else None 
  }
  

  override val fileName: String = oldmeth.owner.sourceFileName
  
  override val description = {
    oldmeth.methodString + (if (newmeths.tail.isEmpty)
      "'s type has changed; was " + oldmeth.tpe + ", is now: " + newmeths.head.tpe
    else
      " does not have a correspondent with same parameter signature among " +
        (newmeths map (_.tpe) mkString ", "))
  }
}

case class IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo) extends Problem {
  
  override val fixHint = {
    if(oldmeth.hasSyntheticName)
      None
    else 
      Some(ui.AddBridgeMethod(oldmeth, newmeth))
  }
  
  override val fileName: String = oldmeth.owner.sourceFileName

  override val description = {
    oldmeth.methodString + " has now a different result type; was: " +
      oldmeth.tpe.resultType + ", is now: " + newmeth.tpe.resultType
  }
}

case class AbstractMethodProblem(newmeth: MemberInfo) extends Problem {
  status = Problem.Status.Upgradable
  override val fileName: String = newmeth.owner.sourceFileName
  override val description = "abstract " + newmeth.methodString + " does not have a correspondent in old version"
}

case class IncompatibleClassDeclarationProblem(oldClazz: ClassInfo, newClazz: ClassInfo) extends Problem {
  override val fileName: String = oldClazz.sourceFileName
  override val description = {
    "declaration of " + oldClazz.description + " has changed to " + newClazz.description +
      " in new version; changing " + oldClazz.declarationPrefix + " to " + newClazz.declarationPrefix + " breaks client code"
  }
}