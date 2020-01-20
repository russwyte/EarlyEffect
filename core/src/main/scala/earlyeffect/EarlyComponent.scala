package earlyeffect

import earlyeffect.impl.EarlyEffect
import org.scalajs.dom

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.{Dictionary, UndefOr}

trait ClassSelector { self: EarlyComponent[_, _] =>
  def selector = s".$defaultKey"

  def addClass(e: dom.Element): Unit = {
    val oldClass = Option(e.getAttribute("class"))
    val newClass = oldClass.fold(defaultKey)(old => {
      if (old.contains(defaultKey)) old else old + " " + defaultKey
    })
    e.setAttribute(
      name = "class",
      value = newClass
    )
  }

}

trait InstanceDataSelector { self: EarlyComponent[_, _] =>
  val attributeName = s"data-earlyeffect-$defaultKey"
  def extractAttributeValue(instance: self.Instance): String
  def selector(attributeValue: String) = s"[$attributeName='$attributeValue']"

  def addDataAttribute(e: dom.Element, instance: Instance): Unit =
    e.setAttribute(attributeName, self.extractAttributeValue(instance))
}

trait EarlyComponent[Props, State] { self =>

  type P = Props

  import dictionaryNames._

  def instanceConstructor: js.Dynamic

  val defaultKey = self.getClass.getName.replaceAll("[^\\w]", "_")

  type Instance = EarlyInstance[Props, State]

  def didMount(instance: Instance): Unit = ()

  def willMount(instance: Instance): Unit = ()

  def willUnMount(instance: Instance): Unit = ()

  def didUpdate(
      oldProps: Props,
      oldState: State,
      instance: Instance,
      oldInstance: UndefOr[Instance]
  ): Unit = ()

  def baseDictionary(props: Props): Dictionary[js.Any] =
    js.Dictionary(
      Seq[(String, js.Any)](
        (PropsFieldName, props.asInstanceOf[js.Any]),
        ("key", defaultKey) // this is a precaution - I may want to make this optional
      ): _*
    )

  def apply(props: Props): VNode = EarlyEffect.h(instanceConstructor, baseDictionary(props))

  def addSelectors(n: VNode, facade: InstanceFacade[Props, State]): VNode =
    self match {
      case classSelector: ClassSelector => n.withRef(e => classSelector.addClass(e))
      case instanceDataSelector: InstanceDataSelector =>
        n.withRef(e => instanceDataSelector.addDataAttribute(e, facade))
      case _ => n
    }

}

object EarlyComponent {
  implicit def toVNode(ec: EarlyComponent[Unit, _]): VNode = ec.apply(())
}
